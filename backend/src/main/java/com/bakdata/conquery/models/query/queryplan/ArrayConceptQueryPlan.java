package com.bakdata.conquery.models.query.queryplan;

import java.util.*;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.forms.util.ResultModifier;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.concept.ArrayConceptQuery;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.SinglelineContainedEntityResult;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents the QueryPlan for corresponding to the {@link ArrayConceptQuery}.
 */
@Getter
@ToString
public class ArrayConceptQueryPlan implements QueryPlan {

	private List<ConceptQueryPlan> childPlans;
	@ToString.Exclude
	private boolean specialDateUnion = false;

	private ArrayConceptQueryPlan(boolean generateSpecialDateUnion) {
		specialDateUnion = generateSpecialDateUnion;
	}

	public ArrayConceptQueryPlan(QueryPlanContext context) {
		this(context.isGenerateSpecialDateUnion());
	}

	public boolean isOfInterest(Bucket bucket) {
		for (ConceptQueryPlan child : childPlans) {
			if (child.isOfInterest(bucket)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public ArrayConceptQueryPlan clone(CloneContext ctx) {
		List<ConceptQueryPlan> childPlanClones = new ArrayList<>();
		for (ConceptQueryPlan child : childPlans) {
			childPlanClones.add(child.clone(ctx));
		}
		ArrayConceptQueryPlan aqClone = new ArrayConceptQueryPlan(specialDateUnion);
		aqClone.childPlans = new ArrayList<>(childPlanClones);
		return aqClone;
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
	}

	public void init(QueryExecutionContext ctx, Entity entity) {
		childPlans.forEach(plan -> plan.init(entity, ctx));
	}

	@Override
	public EntityResult execute(QueryExecutionContext ctx, Entity entity) {

		init(ctx, entity);

		if (!isOfInterest(entity)) {
			return EntityResult.notContained();
		}


		Object[] resultValues = new Object[this.getAggregatorSize()];
		// Start with 1 for aggregator values if dateSet needs to be added to the result
		CDateSet dateSet = CDateSet.create();
		int resultInsertIdx = specialDateUnion ? 1 : 0;
		boolean notContainedInChildQueries = true;
		for (ConceptQueryPlan child : childPlans) {
			SinglelineEntityResult result = child.execute(ctx, entity);

			// Check if child returned a result
			if (!result.isContained()) {
				final Object[] applied = ResultModifier.existAggValuesSetterFor(child.getAggregators(), OptionalInt.of(0)).apply(new Object[child.getAggregatorSize()]);

				// applied[0] is the child-queries DateUnion, which we don't copy.
				int copyLength = applied.length - (specialDateUnion ? 1 : 0);
				System.arraycopy(applied, specialDateUnion ? 1 : 0, resultValues, resultInsertIdx, copyLength);

				// Advance pointer for the result insertion by the number of currently handled
				// aggregators.
				resultInsertIdx = nextIndex(resultInsertIdx, child);
				continue;
			}

			SinglelineContainedEntityResult singleLineResult = (SinglelineContainedEntityResult) result;
			// Mark this result line as contained.
			notContainedInChildQueries = false;
			int srcCopyPos = 0;
			if (specialDateUnion) {
				for(Object dateRange : (Collection<CDateRange>) singleLineResult.getValues()[0]) {
					dateSet.add((CDateRange) dateRange);
				}
				// Skip overwriting the first value: daterange
				srcCopyPos = 1;
			}

			int copyLength = calculateCopyLength(singleLineResult);
			System.arraycopy(singleLineResult.getValues(), srcCopyPos, resultValues, resultInsertIdx, copyLength);

			// Advance pointer for the result insertion by the number of currently handled
			// aggregators.
			resultInsertIdx = nextIndex(resultInsertIdx, child);
		}
		if (notContainedInChildQueries) {
			// None of the subqueries contained an result
			return EntityResult.notContained();
		}

		if (specialDateUnion) {
			// Dateset was needed, add it to the front.
			resultValues[0] = dateSet.asRanges();
		}

		return new SinglelineContainedEntityResult(entity.getId(), resultValues);
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
		return specialDateUnion ? size - childPlans.size() + 1 : size;
	}

	public List<Aggregator<?>> getAggregators() {
		List<Aggregator<?>> aggregators = new ArrayList<>();
		for (ConceptQueryPlan child : childPlans) {
			List<Aggregator<?>> allAggs = child.getAggregators();
			aggregators.addAll(allAggs.subList((specialDateUnion ? 1 : 0), allAggs.size()));
		}

		return aggregators;
	}

	private int nextIndex(int currentIdx, ConceptQueryPlan child) {
		/**
		 * If we have a specialDateUnion, we also have those in the children. We don't
		 * want to add the result directly to the end result (its merged in a single
		 * DateSet). Hence the index for the result insertion is reduces by one.
		 */
		int offset = child.getAggregatorSize() - (specialDateUnion ? 1 : 0);
		if (offset < 0) {
			throw new IllegalStateException("Result index offset must be positive, so the advancing pointer does not override results.");
		}
		return currentIdx + offset;
	}

	private int calculateCopyLength(SinglelineContainedEntityResult singleLineResult) {
		int length = singleLineResult.getValues().length - (specialDateUnion ? 1 : 0);
		if (length < 0) {
			throw new IllegalStateException("Copy length must be positive.");
		}
		return length;
	}

	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		childPlans.forEach(plan -> plan.nextTable(ctx, currentTable));
	}

	public void nextBlock(Bucket bucket) {
		childPlans.forEach(plan -> plan.nextBlock(bucket));
	}
}
