package com.bakdata.conquery.models.forms.managed;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineContainedEntityResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Getter @RequiredArgsConstructor
public class AbsoluteFormQueryPlan implements QueryPlan<MultilineContainedEntityResult> {

	private final QueryPlan query;
	private final FormQueryPlan subPlan;
	
	@Override
	public Optional<MultilineContainedEntityResult> execute(QueryExecutionContext ctx, Entity entity) {
		Optional<EntityResult> preResult = query.execute(ctx, entity);
		if (preResult.isEmpty()) {
			return Optional.empty();
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
	public CDateSet getValidityDates(MultilineContainedEntityResult result) {
		return subPlan.getValidityDates(result);
	}
}
