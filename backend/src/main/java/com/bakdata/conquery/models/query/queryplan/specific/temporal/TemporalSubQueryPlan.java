package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import java.util.ArrayList;
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

		final CDateRange[] partitions = indexSelector.sample(indexSubPlan.getDateAggregator().createAggregationResult());
		final boolean[] results = new boolean[partitions.length];
		final CDateRange[] convertedPartitions = indexMode.convert(partitions, CDateRange::getMinValue, indexSelector);

		final List<List<Aggregator<?>>> collectedResults = new ArrayList<>();

		log.trace("Querying {} for {} => {}", entity, partitions, convertedPartitions);

		for (int index = 0; index < convertedPartitions.length; index++) {
			final CDateRange subPeriod = convertedPartitions[index];

			// First execute sub-query with before's sub-period to extract after's sub-periods which are then used to evaluate after.
			final Optional<CDateSet> resultDate = evaluateReference(ctx, entity, subPeriod)
					.map(cqp -> cqp.getDateAggregator().createAggregationResult());

			if (resultDate.isEmpty()) {
				continue;
			}

			final CDateRange[] afterSampled = compareSelector.sample(resultDate.get());
			final boolean[] subResults = new boolean[afterSampled.length];
			final ConceptQueryPlan[] subPlans = new ConceptQueryPlan[afterSampled.length];

			for (int innerIndex = 0; innerIndex < afterSampled.length; innerIndex++) {
				final CDateRange dateRange = afterSampled[innerIndex];
				// Execute after-query to get actual result
				final Optional<ConceptQueryPlan> afterResultDate = evaluateReference(ctx, entity, dateRange);

				subPlans[innerIndex] = afterResultDate.orElse(null);

				subResults[innerIndex] = afterResultDate.isPresent();
			}


			if (compareSelector.satisfies(subResults)) {
				results[index] = true;
				dateResult.add(subPeriod);

				for (ConceptQueryPlan subPlan : subPlans) {
					if (subPlan == null) {
						continue;
					}
					collectedResults.add(subPlan.getAggregators());
				}
			}
		}

		for (List<Aggregator<?>> result : collectedResults) {
			for (int aggIdx = 0; aggIdx + 1 < result.size(); aggIdx++) {
				aggregationResults.get(aggIdx).add(result.get(aggIdx + 1 /* skips dateAggregator */).createAggregationResult());
			}
		}

		final boolean satisfies = indexSelector.satisfies(results);

		if (!satisfies) {
			return Optional.empty();
		}

		return Optional.of(new SinglelineEntityResult(entity.getId(), null));
	}

	private Optional<ConceptQueryPlan> evaluateReference(QueryExecutionContext ctx, Entity entity, CDateRange partition) {

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
