package com.bakdata.conquery.models.forms.managed;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter @RequiredArgsConstructor
public class AbsoluteFormQueryPlan implements QueryPlan {

	private final QueryPlan query;
	private final FormQueryPlan subPlan;
	
	@Override
	public EntityResult execute(QueryExecutionContext ctx, Entity entity) {
		EntityResult preResult = query.execute(ctx, entity);
		if (preResult.isFailed() || !preResult.isContained()) {
			return preResult;
		}
		return subPlan.execute(ctx, entity);
	}

	@Override
	public AbsoluteFormQueryPlan clone(CloneContext ctx) {
		return new AbsoluteFormQueryPlan(
			query.clone(ctx),
			subPlan
		);
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return query.isOfInterest(entity);
	}

	@Override
	public CDateSet getValidityDates(ContainedEntityResult result) {
		return subPlan.getValidityDates(result);
	}
}
