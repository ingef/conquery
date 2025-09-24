package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ConstantValueAggregator;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = false)
@Slf4j
@Data
public class TemporalQueryNode extends QPNode {

	/**
	 * We need the AllIds-Table here, to ensure we get evaluated.
	 */
	private final Table table;

	private final ConceptQueryPlan indexQueryPlan;
	private final TemporalSelector indexSelector;
	private final TemporalRelationMode indexMode;

	private final ConceptQueryPlan outerCompareQueryPlan;
	private final ConceptQueryPlan innerCompareQueryPlan;

	private final TemporalSelector compareSelector;


	private final List<List> aggregationResults;
	private final ConstantValueAggregator<CDateSet> compareDateAggregator = new ConstantValueAggregator<>(null);
	private final ConstantValueAggregator<CDateSet> indexDateAggregator = new ConstantValueAggregator<>(null);

	private CDateSet indexDateResult;

	private CDateSet compareDateResult;

	private boolean result;

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		super.init(entity, context);

		indexQueryPlan.init(context, entity);

		aggregationResults.forEach(List::clear);

		indexDateResult = CDateSet.createEmpty();
		compareDateResult = CDateSet.createEmpty();

		compareDateAggregator.setValue(compareDateResult);
		indexDateAggregator.setValue(indexDateResult);

		result = evaluateSubQueries(context, entity);
	}

	public boolean evaluateSubQueries(QueryExecutionContext ctx, Entity entity) {

		final Optional<SinglelineEntityResult> subResult = indexQueryPlan.execute(ctx, entity);

		if (subResult.isEmpty()) {
			return false;
		}

		final CDateRange[] periods = indexSelector.sample(indexQueryPlan.getDateAggregator().createAggregationResult());
		final CDateRange[] indexPeriods = indexMode.convert(periods, indexSelector);

		final boolean[] results = new boolean[indexPeriods.length];

		// First execute sub-query with index's sub-period
		// to extract compares sub-periods which are then used to evaluate compare for aggregation/inclusion.
		for (int current = 0; current < indexPeriods.length; current++) {
			final CDateRange indexPeriod = indexPeriods[current];

			if (indexPeriod == null) {
				continue;
			}

			// Execute only event-filter based
			final Optional<CDateSet> maybeComparePeriods = evaluateCompareQuery(ctx, entity, indexPeriod, innerCompareQueryPlan)
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
				final Optional<ConceptQueryPlan> compareResult = evaluateCompareQuery(ctx, entity, comparePeriod, outerCompareQueryPlan);

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

		return indexSelector.satisfies(results);
	}

	private Optional<ConceptQueryPlan> evaluateCompareQuery(QueryExecutionContext ctx, Entity entity, CDateRange partition, ConceptQueryPlan cqp) {
		ctx = ctx.withDateRestriction(CDateSet.create(partition));

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
	public void collectRequiredTables(Set<Table> requiredTables) {
		requiredTables.add(table);
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return indexQueryPlan.isOfInterest(entity);
	}


	@Override
	public boolean acceptEvent(Bucket bucket, int event) {
		// Does nothing
		return false;
	}

	@Override
	public boolean isContained() {
		return result;
	}

	@Override
	public Collection<Aggregator<CDateSet>> getDateAggregators() {
		return List.of(indexDateAggregator);
	}
}
