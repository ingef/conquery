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
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ConstantValueAggregator;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Data
@Slf4j
public class TemporalSubQueryPlan implements QueryPlan<EntityResult> {

	private final CQTemporal.Selector indexSelector;
	private final CQTemporal.Mode indexMode;

	private final CQTemporal.Selector compareSelector;

	private final CQElement compareQuery;

	private final QueryPlanContext queryPlanContext;

	private final ConceptQueryPlan indexSubPlan;


	private final List<List> aggregationResults;


	private CDateSet dateResult;


	@Override
	public void init(QueryExecutionContext ctx, Entity entity) {
		aggregationResults.forEach(List::clear);

		indexSubPlan.init(ctx, entity);

		dateResult = CDateSet.createEmpty();
	}

	@Override
	public Optional<EntityResult> execute(QueryExecutionContext ctx, Entity entity) {


		final Optional<SinglelineEntityResult> subResult = indexSubPlan.execute(ctx, entity);

		if (subResult.isEmpty()) {
			return Optional.empty();
		}

		// I use arrays here as they are much easier to keep aligned and their size is known ahead of time
		final CDateRange[] periods = indexSelector.sample(indexSubPlan.getDateAggregator().createAggregationResult());
		final boolean[] results = new boolean[periods.length];
		final CDateRange[] convertedPeriods = indexMode.convert(periods, CDateRange::getMinValue, indexSelector);

		assert periods.length == convertedPeriods.length;

		log.trace("Querying {} for {} => {}", entity, periods, convertedPeriods);

		// First execute sub-query with index's sub-period
		// to extract compares's sub-periods which are then used to evaluate compare for aggregation/inclusion.
		for (int current = 0; current < convertedPeriods.length; current++) {
			final CDateRange indexPeriod = convertedPeriods[current];

			final Optional<CDateSet> resultDate = evaluateCompareQuery(ctx, entity, indexPeriod)
					.map(cqp -> cqp.getDateAggregator().createAggregationResult());

			if (resultDate.isEmpty()) {
				continue;
			}

			final CDateRange[] compareSampled = compareSelector.sample(resultDate.get());
			final boolean[] subResults = new boolean[compareSampled.length];
			final ConceptQueryPlan[] subPlans = new ConceptQueryPlan[compareSampled.length];

			for (int inner = 0; inner < compareSampled.length; inner++) {
				final CDateRange dateRange = compareSampled[inner];
				// Execute compare-query to get actual result
				final Optional<ConceptQueryPlan> afterResultDate = evaluateCompareQuery(ctx, entity, dateRange);

				subPlans[inner] = afterResultDate.orElse(null);
				subResults[inner] = afterResultDate.isPresent();
			}

			// If compare's selector is satisfied, we append current to the results and retrieve the aggregation results
			if (!compareSelector.satisfies(subResults)) {
				continue;
			}

			results[current] = true;
			dateResult.add(indexPeriod);

			for (ConceptQueryPlan subPlan : subPlans) {
				if (subPlan == null) {
					continue;
				}

				addAggregationResults(subPlan);
			}
		}

		final boolean satisfies = indexSelector.satisfies(results);

		if (!satisfies) {
			return Optional.empty();
		}

		return Optional.of(new SinglelineEntityResult(entity.getId(), null));
	}

	private void addAggregationResults(ConceptQueryPlan subPlan) {
		final List<Aggregator<?>> result = subPlan.getAggregators();

		for (int aggIdx = 0; aggIdx + 1 < result.size(); aggIdx++) {
			aggregationResults.get(aggIdx).add(result.get(aggIdx + 1 /* skips dateAggregator */).createAggregationResult());
		}
	}

	private Optional<ConceptQueryPlan> evaluateCompareQuery(QueryExecutionContext ctx, Entity entity, CDateRange partition) {

		final ConceptQuery query = new ConceptQuery(new CQDateRestriction(partition.toSimpleRange(), compareQuery));

		// Execute after-query to get result date only
		final ConceptQueryPlan cqp = query.createQueryPlan(queryPlanContext);

		cqp.init(ctx, entity);

		final Optional<SinglelineEntityResult> entityResult = cqp.execute(ctx, entity);

		return entityResult.map(ignored -> cqp);
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return indexSubPlan.isOfInterest(entity);
	}

	@NotNull
	@Override
	public Optional<Aggregator<CDateSet>> getValidityDateAggregator() {
		return Optional.of(new ConstantValueAggregator<>(dateResult, new ResultType.ListT(ResultType.DateT.INSTANCE)));
	}
}
