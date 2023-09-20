package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import java.util.Optional;

import com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQTemporal;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ConstantValueAggregator;
import com.bakdata.conquery.models.query.queryplan.specific.DateRestrictingNode;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class TemporalSubQueryPlan implements QueryPlan<EntityResult> {

	private final CQTemporal.Selector beforeSelector;
	private final CQTemporal.Mode beforeMode;

	private final CQTemporal.Selector afterSelector;

	private final QPNode before;

	private final QPNode after;

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
	public Optional execute(QueryExecutionContext ctx, Entity entity) {


		final Optional<SinglelineEntityResult> subResult = beforePlan.execute(ctx, entity);

		if (subResult.isEmpty()) {
			return Optional.empty();
		}

		final CDateRange[] partitions = beforeSelector.sample(beforePlan.getDateAggregator().createAggregationResult());
		final boolean[] results = new boolean[partitions.length];
		final CDateRange[] convertedPartitions = beforeMode.convert(partitions, CDateRange::getMinValue);


		for (int index = 0; index < convertedPartitions.length; index++) {
			final CDateRange subPeriod = convertedPartitions[index];

			// First execute sub-query with before's sub-period to extract after's sub-periods which are then used to evaluate after.
			final Optional<CDateSet> resultDate = evaluateAfterFor(ctx, entity, subPeriod);

			if (resultDate.isEmpty()) {
				continue;
			}

			final CDateRange[] afterSampled = afterSelector.sample(resultDate.get());
			final boolean[] subResults = new boolean[afterSampled.length];

			for (int innerIndex = 0; innerIndex < afterSampled.length; innerIndex++) {
				final CDateRange dateRange = afterSampled[innerIndex];
				// Execute after-query to get actual result
				final Optional<CDateSet> afterResultDate = evaluateAfterFor(ctx, entity, dateRange);

				subResults[innerIndex] = afterResultDate.isPresent();
			}

			//TODO somehow ensure that the last successful query is returned in the output.

			if (afterSelector.satisfies(subResults)) {
				results[index] = true;
				result.add(subPeriod);
			}
		}

		final boolean satisfies = beforeSelector.satisfies(results);

		if (!satisfies) {
			return Optional.empty();
		}

		return Optional.of(new SinglelineEntityResult(entity.getId(), null));
	}

	private Optional<CDateSet> evaluateAfterFor(QueryExecutionContext ctx, Entity entity, CDateRange partition) {

		// Execute after-query to get result date only
		final ConceptQueryPlan cqp = new ConceptQueryPlan(true);

		cqp.setChild(new DateRestrictingNode(CDateSet.create(partition), after));
		cqp.getDateAggregator().registerAll(after.getDateAggregators());

		cqp.init(ctx, entity);

		final Optional<SinglelineEntityResult> entityResult = cqp.execute(ctx, entity);

		return entityResult.map(ignored -> cqp.getDateAggregator().createAggregationResult());
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
