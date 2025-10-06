package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import java.util.ArrayList;
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


	private final List<ConstantValueAggregator<List>> aggregationResults;
	private final ConstantValueAggregator<CDateSet> compareDateAggregator = new ConstantValueAggregator<>(null);
	private final ConstantValueAggregator<CDateSet> indexDateAggregator = new ConstantValueAggregator<>(null);

	private CDateSet indexDateResult;

	private CDateSet compareDateResult;

	private boolean result = false;
	private boolean hit = false;


	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		super.init(entity, context);

		indexQueryPlan.init(context, entity);
		outerCompareQueryPlan.init(context, entity);
		innerCompareQueryPlan.init(context, entity);

		aggregationResults.forEach(agg -> agg.setValue(new ArrayList()));

		indexDateResult = CDateSet.createEmpty();
		compareDateResult = CDateSet.createEmpty();

		compareDateAggregator.setValue(compareDateResult);
		indexDateAggregator.setValue(indexDateResult);

		result = false;
		hit = false;
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
		if (hit) {
			return false;
		}

		hit = true;

		result = evaluateTemporalQuery(context, entity);

		// Does nothing
		return false;
	}

	/**
	 * Evaluate indexQueryPlan for indexPeriods.
	 * Use result-dates with selector and mode to evaluate outerCompareQueryPlan, which produces subPeriods, to evaluate innerCompareQueryPlan.
	 * <br />
	 * outer- and innerCompareQueryPlan are identical, except that outer has all aggregations disabled, we are only interested in the periods, where inner _might_ be true.
	 * Outer is evaluated with aggregations, to produce correct results.
	 * <br />
	 * mode and selector filter periods to the relevant timestamps and eventually determine if an entity is included or not.
	 */
	public boolean evaluateTemporalQuery(QueryExecutionContext ctx, Entity entity) {

		QueryExecutionContext indexCtx = ctx.withQueryDateAggregator(Optional.empty());
		indexQueryPlan.init(indexCtx, entity);
		indexQueryPlan.execute(indexCtx, entity);

		if (!indexQueryPlan.isContained()) {
			return false;
		}

		final CDateRange[] periods = indexSelector.sample(indexQueryPlan.getDateAggregator().createAggregationResult());
		final CDateRange[] indexPeriods = indexMode.convert(periods, indexSelector);

		final boolean[] results = new boolean[indexPeriods.length];

		// First execute sub-query with index's sub-period
		// to extract compares sub-periods which are then used to evaluate compare for aggregation/inclusion.
		for (int current = 0; current < indexPeriods.length; current++) {
			final CDateRange indexPeriod = indexPeriods[current];

			// Execute only event-filter based, to get potential periods for inner, with aggregation.
			if (!evaluateWithRestriction(entity, indexPeriod, outerCompareQueryPlan, ctx)) {
				continue;
			}

			final CDateSet outerDates = outerCompareQueryPlan.getDateAggregator().createAggregationResult();
			final CDateRange[] comparePeriods = compareSelector.sample(outerDates);

			final boolean[] compareContained = new boolean[comparePeriods.length];
			final CDateSet[] compareDates = new CDateSet[comparePeriods.length];
			final Object[][] compareAggregationResults = new Object[comparePeriods.length][];

			for (int inner = 0; inner < comparePeriods.length; inner++) {

				final CDateRange comparePeriod = comparePeriods[inner];

				// Execute compare-query to get actual result
				compareContained[inner] = evaluateWithRestriction(entity, comparePeriod, innerCompareQueryPlan, ctx);

				if (compareContained[inner]) {
					compareDates[inner] = innerCompareQueryPlan.getDateAggregator().createAggregationResult();
					compareAggregationResults[inner] = innerCompareQueryPlan.getAggregators().stream().skip(1).map(Aggregator::createAggregationResult).toArray();
				}
			}

			boolean satisfies = compareSelector.satisfies(compareContained);

			log.debug("{}:{} => indexPeriod={}, comparePeriods={}, compareContained={} => {}",
					  getEntity(),
					  compareSelector,
					  indexPeriod,
					  comparePeriods,
					  compareContained,
					  satisfies
			);

			// If compare's selector is satisfied, we append current to the results and collect the aggregation results
			if (!satisfies) {
				continue;
			}

			results[current] = true;
			indexDateResult.add(periods[current]);

			addAggregationResults(compareContained, compareDates, compareAggregationResults);
		}

		boolean satisfies = indexSelector.satisfies(results);

		log.debug("{}:{} => indexPeriods={}, results={} => {}", getEntity(), indexSelector, indexPeriods, results, satisfies);


		return satisfies;
	}

	/**
	 * Apply date-restriction to QueryContext, and execute Query.
	 *
	 * @return if the entity is contained or not.
	 */
	private static boolean evaluateWithRestriction(Entity entity, CDateRange partition, ConceptQueryPlan cqp, QueryExecutionContext ctx) {
		ctx = ctx.withDateRestriction(CDateSet.create(partition))
				 .withQueryDateAggregator(Optional.empty());

		cqp.init(ctx, entity);
		cqp.execute(ctx, entity);

		return cqp.isContained();
	}

	/**
	 * Collect aggregation results of satisfied innerQueries.
	 */
	@SuppressWarnings("unchecked")
	private void addAggregationResults(boolean[] compareContained, CDateSet[] compareDates, Object[][] compareAggregationResults) {
		assert compareContained.length == compareDates.length;
		assert compareContained.length == compareAggregationResults.length;

		for (int index = 0; index < compareContained.length; index++) {
			if (!compareContained[index]) {
				continue;
			}

			compareDateResult.addAll(compareDates[index]);
			for (int aggIndex = 0; aggIndex < aggregationResults.size(); aggIndex++) {

				aggregationResults.get(aggIndex)
								  .getValue()
								  .add(compareAggregationResults[index][aggIndex]);
			}
		}
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
