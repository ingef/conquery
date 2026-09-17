package com.bakdata.conquery.models.forms.managed;

import java.util.Optional;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter @RequiredArgsConstructor
@ToString
public class AbsoluteFormQueryPlan implements QueryPlan<MultilineEntityResult> {

	private final QueryPlan query;
	private final FormQueryPlan subPlan;

	@Override
	public void init(QueryExecutionContext ctxt, Entity entity) {
		query.init(ctxt, entity);
		subPlan.init(ctxt, entity);
	}

	@Override
	public Optional<MultilineEntityResult> execute(QueryExecutionContext ctx, Entity entity) {

		// Don't set the query date aggregator here because the subqueries should set their aggregator independently

		Optional<EntityResult> preResult = query.execute(ctx, entity);

		if (preResult.isEmpty()) {
			return Optional.empty();
		}
		return subPlan.execute(ctx, entity);
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return query.isOfInterest(entity);
	}

	@Override
	public Optional<Aggregator<CDateSet>> getValidityDateAggregator() {
		return subPlan.getValidityDateAggregator();
	}
}
