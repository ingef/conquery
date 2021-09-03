package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.forms.util.ResultModifier;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import com.bakdata.conquery.util.QueryUtils;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents the QueryPlan for corresponding to the {@link ArrayConceptQuery}.
 */
@Getter
@ToString
public class ArrayConceptQueryPlan implements QueryPlan<SinglelineEntityResult> {

	public static final int VALIDITY_DATE_POSITION = 0;
	private List<ConceptQueryPlan> childPlans;
	@ToString.Exclude
	private boolean generateDateAggregation;
	private final DateAggregator validityDateAggregator = new DateAggregator(DateAggregationAction.MERGE);

	public ArrayConceptQueryPlan(boolean generateDateAggregation) {
		this.generateDateAggregation = generateDateAggregation;
	}

	public boolean isOfInterest(Bucket bucket) {
		for (ConceptQueryPlan child : childPlans) {
			if (child.isOfInterest(bucket)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Helper function to add child queries. This takes care of the SpecialDateUnion
	 * union handling. It acts as a gate keeper, so all child queries either have a
	 * SpecialDateUnion or none.
	 *
	 * @param childQueries The queries that are individually executed, for which QueryPlans
	 *                     are generated uniformly regarding the SpecialDateContext.
	 * @param context      Primarily used to decide if a SpecialDateUnion needs to be
	 *                     generated.
	 */
	public void addChildPlans(List<ConceptQuery> childQueries, QueryPlanContext context) {

		childPlans = new ArrayList<>();
		for (ConceptQuery child : childQueries) {
			childPlans.add(child.createQueryPlan(context));
		}

		if (generateDateAggregation) {
			initDateAggregator(validityDateAggregator, childPlans);
		}
	}

	private static void initDateAggregator(DateAggregator validityDateAggregator, List<ConceptQueryPlan> childPlans) {
		for (ConceptQueryPlan plan : childPlans) {
			plan.getValidityDateAggregator().ifPresent(validityDateAggregator::register);
		}
	}

	@Override
	public void init(QueryExecutionContext ctx, Entity entity) {
		validityDateAggregator.init(entity, ctx);
		childPlans.forEach(child -> child.init(ctx,entity));
	}

	@Override
	public Optional<SinglelineEntityResult> execute(QueryExecutionContext ctx, Entity entity) {


		// Only override if none has been set from a higher level
		ctx = QueryUtils.determineDateAggregatorForContext(ctx, this::getValidityDateAggregator);

		if (!isOfInterest(entity)) {
			return Optional.empty();
		}


		Object[] resultValues = new Object[getAggregatorSize()];
		// Start with 1 for aggregator values if dateSet needs to be added to the result
		final int  resultOffset = generateDateAggregation ? 1 : 0;
		int resultInsertIdx = resultOffset;
		boolean containedInChildQueries = false;

		for (ConceptQueryPlan child : childPlans) {
			Optional<SinglelineEntityResult> result = child.execute(ctx, entity);

			if (result.isEmpty()) {
				// The sub result was empty. Generate the necessary gapped columns in the result line
				final Object[] applied = ResultModifier.existAggValuesSetterFor(child.getAggregators(), OptionalInt.of(0)).apply(new Object[child.getAggregatorSize()]);

				// applied[0] is the child-queries DateUnion, which we don't copy.
				int copyLength = applied.length - resultOffset;
				System.arraycopy(applied, resultOffset, resultValues, resultInsertIdx, copyLength);

				// Advance pointer for the result insertion by the number of currently handled
				// aggregators.
				resultInsertIdx = nextIndex(resultInsertIdx, child);
				continue;
			}

			SinglelineEntityResult singleLineResult = result.get();
			// Mark this result line as contained.
			containedInChildQueries = true;

			int copyLength = calculateCopyLength(singleLineResult);
			System.arraycopy(singleLineResult.getValues(), resultOffset, resultValues, resultInsertIdx, copyLength);

			// Advance pointer for the result insertion by the number of currently handled
			// aggregators.
			resultInsertIdx = nextIndex(resultInsertIdx, child);
		}

		if (!containedInChildQueries) {
			// None of the subqueries contained an result
			return Optional.empty();
		}

		if (generateDateAggregation) {
			// Dateset was needed, add it to the front.
			resultValues[VALIDITY_DATE_POSITION] = validityDateAggregator.createAggregationResult();
		}

		return Optional.of(new SinglelineEntityResult(entity.getId(), resultValues));
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		for (ConceptQueryPlan child : childPlans) {
			if (child.isOfInterest(entity)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Optional<Aggregator<CDateSet>> getValidityDateAggregator() {
		if(!generateDateAggregation) {
			return Optional.empty();
		}


		return Optional.of(validityDateAggregator);
	}


	public int getAggregatorSize() {
		int size = 0;
		for (ConceptQueryPlan child : childPlans) {
			size += child.getAggregatorSize();
		}
		/**
		 * With the specialDateUnion all our children have such an aggregator too (taken
		 * care of the addChildPlans() method). Because the end result should only have
		 * one such column we substract the number of queries from the aggregator size
		 * and add one for the union present in this class.
		 */
		return generateDateAggregation ? size - childPlans.size() + 1 : size;
	}

	public List<Aggregator<?>> getAggregators() {
		List<Aggregator<?>> aggregators = new ArrayList<>();
		for (ConceptQueryPlan child : childPlans) {
			List<Aggregator<?>> allAggs = child.getAggregators();
			aggregators.addAll(allAggs.subList((generateDateAggregation ? 1 : 0), allAggs.size()));
		}

		return aggregators;
	}

	private int nextIndex(int currentIdx, ConceptQueryPlan child) {
		/**
		 * If we have a specialDateUnion, we also have those in the children. We don't
		 * want to add the result directly to the end result (its merged in a single
		 * DateSet). Hence the index for the result insertion is reduces by one.
		 */
		int offset = child.getAggregatorSize() - (generateDateAggregation ? 1 : 0);
		if (offset < 0) {
			throw new IllegalStateException("Result index offset must be positive, so the advancing pointer does not override results.");
		}
		return currentIdx + offset;
	}

	private int calculateCopyLength(SinglelineEntityResult singleLineResult) {
		int length = singleLineResult.getValues().length - (generateDateAggregation ? 1 : 0);
		if (length < 0) {
			throw new IllegalStateException("Copy length must be positive.");
		}
		return length;
	}

	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		childPlans.forEach(plan -> plan.nextTable(ctx, currentTable));
	}

	public void nextBlock(Bucket bucket) {
		childPlans.forEach(plan -> plan.nextBlock(bucket));
	}
}
