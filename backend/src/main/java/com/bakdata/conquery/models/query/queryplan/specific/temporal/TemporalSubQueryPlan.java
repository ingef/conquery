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
	private final ConstantValueAggregator<CDateSet> compareDateAggregator = new ConstantValueAggregator<>(null);
	private final ConstantValueAggregator<CDateSet> indexDateAggregator = new ConstantValueAggregator<>(null);

	private CDateSet indexDateResult;

	private CDateSet compareDateResult;

	@Override
	public void init(QueryExecutionContext ctx, Entity entity) {
		indexSubPlan.init(ctx, entity);

		aggregationResults.forEach(List::clear);

		indexDateResult = CDateSet.createEmpty();
		compareDateResult = CDateSet.createEmpty();

		compareDateAggregator.setValue(compareDateResult);
		indexDateAggregator.setValue(indexDateResult);
	}

	@Override
	public Optional<EntityResult> execute(QueryExecutionContext ctx, Entity entity) {

		final Optional<SinglelineEntityResult> subResult = indexSubPlan.execute(ctx, entity);

		if (subResult.isEmpty()) {
			return Optional.empty();
		}

		// I use arrays here as they are much easier to keep aligned
		final CDateRange[] periods = indexSelector.sample(indexSubPlan.getDateAggregator().createAggregationResult());
		final CDateRange[] indexPeriods = indexMode.convert(periods, indexSelector);

		final boolean[] results = new boolean[indexPeriods.length];

		log.trace("Querying {} for {} => {}", entity, periods, indexPeriods);

		// First execute sub-query with index's sub-period
		// to extract compares sub-periods which are then used to evaluate compare for aggregation/inclusion.
		for (int current = 0; current < indexPeriods.length; current++) {
			final CDateRange indexPeriod = indexPeriods[current];

			if (indexPeriod == null) {
				continue;
			}

			// Execute only event-filter based
			final Optional<CDateSet> maybeComparePeriods = evaluateCompareQuery(ctx, entity, indexPeriod, true)
					.map(cqp -> cqp.getDateAggregator().createAggregationResult());

			if (maybeComparePeriods.isEmpty()) {
				continue;
			}

			final CDateRange[] comparePeriods = compareSelector.sample(maybeComparePeriods.get());

			final boolean[] compareResults = new boolean[comparePeriods.length];
			final ConceptQueryPlan[] compareSubPlans = new ConceptQueryPlan[comparePeriods.length];

			for (int inner = 0; inner < comparePeriods.length; inner++) {
				final CDateRange comparePeriod = comparePeriods[inner];
				// Execute compare-query to get actual result
				final Optional<ConceptQueryPlan> compareResult = evaluateCompareQuery(ctx, entity, comparePeriod, false);

				compareSubPlans[inner] = compareResult.orElse(null);
				compareResults[inner] = compareResult.isPresent();
			}

			// If compare's selector is satisfied, we append current to the results and retrieve the aggregation results
			if (!compareSelector.satisfies(compareResults)) {
				continue;
			}

			results[current] = true;
			indexDateResult.add(periods[current]);

			for (ConceptQueryPlan subPlan : compareSubPlans) {
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

	private Optional<ConceptQueryPlan> evaluateCompareQuery(QueryExecutionContext ctx, Entity entity, CDateRange partition, boolean isOuter) {

		final ConceptQuery query = new ConceptQuery(new CQDateRestriction(partition.toSimpleRange(), compareQuery));

		// Execute after-query to get result date only
		final ConceptQueryPlan cqp = query.createQueryPlan(queryPlanContext.withDisableAggregators(isOuter)
																		   .withDisableAggregationFilters(isOuter));

		cqp.init(ctx, entity);

		final Optional<SinglelineEntityResult> entityResult = cqp.execute(ctx, entity);

		return entityResult.map(ignored -> cqp);
	}

	private void addAggregationResults(ConceptQueryPlan subPlan) {
		compareDateResult.addAll(subPlan.getDateAggregator().createAggregationResult());

		final List<Aggregator<?>> result = subPlan.getAggregators();

		for (int aggIdx = 0; aggIdx + 1 < result.size(); aggIdx++) {
			aggregationResults.get(aggIdx).add(result.get(aggIdx + 1 /* skips dateAggregator */).createAggregationResult());
		}
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return indexSubPlan.isOfInterest(entity);
	}

	@NotNull
	@Override
	public Optional<Aggregator<CDateSet>> getValidityDateAggregator() {
		return Optional.of(indexDateAggregator);
	}
}
