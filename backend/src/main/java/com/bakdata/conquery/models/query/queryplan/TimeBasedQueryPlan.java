package com.bakdata.conquery.models.query.queryplan;

import java.util.Collection;
import java.util.Optional;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.TemporalSubQueryPlan;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.Data;

@Data
public class TimeBasedQueryPlan implements QueryPlan<EntityResult> {
	private QueryPlan<EntityResult> subQuery;
	private Collection<TemporalSubQueryPlan> temporalSubPlans;

	@Override
	public void init(QueryExecutionContext ctxt, Entity entity) {
		subQuery.init(ctxt, entity);
		temporalSubPlans.forEach(query -> query.init(ctxt, entity));
	}

	@Override
	public Optional<EntityResult> execute(QueryExecutionContext ctx, Entity entity) {
		for (TemporalSubQueryPlan subQueryPlan : temporalSubPlans) {
			final Optional<?> result = subQueryPlan.execute(ctx, entity);

			if (result.isPresent()) {
				ctx.getTemporalQueryResult().put(subQueryPlan.getRef(),
												 subQueryPlan.getValidityDateAggregator()
															 .map(Aggregator::createAggregationResult)
															 .orElseGet(CDateSet::createEmpty));
			}
		}

		return subQuery.execute(ctx, entity);
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return subQuery.isOfInterest(entity);
	}

	@Override
	public Optional<Aggregator<CDateSet>> getValidityDateAggregator() {
		return Optional.empty();
	}
}
