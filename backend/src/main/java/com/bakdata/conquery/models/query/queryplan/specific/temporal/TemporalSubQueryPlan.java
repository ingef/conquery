package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.CQDateRestriction;
import com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQTemporal;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ConstantValueAggregator;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

@Data
@Slf4j
public class TemporalSubQueryPlan implements QueryPlan<EntityResult> {

	private final CQTemporal.Selector beforeSelector;
	private final CQTemporal.Mode beforeMode;

	private final CQTemporal.Selector afterSelector;

	private final QPNode before;

	private final CQElement after;

	private final QueryPlanContext queryPlanContext;

	private final List<ConstantValueAggregator> aggregators;

	private ConceptQueryPlan beforePlan;

	private CDateSet result;


	@Override
	public void init(QueryExecutionContext ctx, Entity entity) {
		beforePlan = new ConceptQueryPlan(true);

		beforePlan.setChild(before);
		beforePlan.getDateAggregator().registerAll(before.getDateAggregators());

		beforePlan.init(ctx, entity);

		result = CDateSet.createEmpty();
	}

	@Override
	public Optional<EntityResult> execute(QueryExecutionContext ctx, Entity entity) {


		final Optional<SinglelineEntityResult> subResult = beforePlan.execute(ctx, entity);

		if (subResult.isEmpty()) {
			return Optional.empty();
		}

		final CDateRange[] partitions = beforeSelector.sample(beforePlan.getDateAggregator().createAggregationResult());
		final boolean[] results = new boolean[partitions.length];
		final CDateRange[] convertedPartitions = beforeMode.convert(partitions, CDateRange::getMinValue, beforeSelector);

		log.trace("Querying {} for {} => {}", entity, partitions, convertedPartitions);

		for (int index = 0; index < convertedPartitions.length; index++) {
			final CDateRange subPeriod = convertedPartitions[index];

			// First execute sub-query with before's sub-period to extract after's sub-periods which are then used to evaluate after.
			final Optional<CDateSet> resultDate = evaluateReference(ctx, entity, subPeriod)
					.map(cqp -> cqp.getDateAggregator().createAggregationResult());

			if (resultDate.isEmpty()) {
				continue;
			}

			final CDateRange[] afterSampled = afterSelector.sample(resultDate.get());
			final boolean[] subResults = new boolean[afterSampled.length];
			final ConceptQueryPlan[] subPlans = new ConceptQueryPlan[afterSampled.length];

			for (int innerIndex = 0; innerIndex < afterSampled.length; innerIndex++) {
				final CDateRange dateRange = afterSampled[innerIndex];
				// Execute after-query to get actual result
				final Optional<ConceptQueryPlan> afterResultDate = evaluateReference(ctx, entity, dateRange);

				subPlans[innerIndex] = afterResultDate.orElse(null);

				subResults[innerIndex] = afterResultDate.isPresent();
			}


			if (afterSelector.satisfies(subResults)) {
				results[index] = true;
				result.add(subPeriod);

				//TODO make list
				//TODO how does this interact with negation?
				replaceAggregatorResults(subResults, subPlans);
			}
		}

		final boolean satisfies = beforeSelector.satisfies(results);

		if (!satisfies) {
			return Optional.empty();
		}

		return Optional.of(new SinglelineEntityResult(entity.getId(), null));
	}

	private void replaceAggregatorResults(boolean[] subResults, ConceptQueryPlan[] subPlans) {
		// TODO this can theoretically also be parametrised to first/last etc
		final int lastSuccess = ArrayUtils.lastIndexOf(subResults, true);
		final List<Aggregator<?>> subAggs = subPlans[lastSuccess].getAggregators();

		for (int aggIdx = 0; aggIdx < aggregators.size(); aggIdx++) {
			aggregators.get(aggIdx).setValue(subAggs.get(aggIdx + 1).createAggregationResult());
		}
	}

	private Optional<ConceptQueryPlan> evaluateReference(QueryExecutionContext ctx, Entity entity, CDateRange partition) {

		final ConceptQuery query = new ConceptQuery(new CQDateRestriction(partition.toSimpleRange(), after));

		// Execute after-query to get result date only
		final ConceptQueryPlan cqp = query.createQueryPlan(queryPlanContext);

		cqp.init(ctx, entity);

		final Optional<SinglelineEntityResult> entityResult = cqp.execute(ctx, entity);

		return entityResult.map(ignored -> cqp);
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return before.isOfInterest(entity);
	}

	@NotNull
	@Override
	public Optional<Aggregator<CDateSet>> getValidityDateAggregator() {
		return Optional.of(new ConstantValueAggregator<>(result, new ResultType.ListT(ResultType.DateT.INSTANCE)));
	}
}
