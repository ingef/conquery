package com.bakdata.conquery.models.forms.managed;

import java.util.List;

import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.ArrayConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter @RequiredArgsConstructor
public class AbsoluteFormQueryPlan implements QueryPlan {

	private final ConceptQueryPlan query;
	private final List<DateContext> dateContexts;
	private final ArrayConceptQueryPlan features;
	
	@Override
	public EntityResult execute(QueryExecutionContext ctx, Entity entity) {
		EntityResult preResult = query.execute(ctx, entity);
		if (preResult.isFailed() || !preResult.isContained()) {
			return preResult;
		}
		FormQueryPlan subPlan = new FormQueryPlan(dateContexts, features);
		return subPlan.execute(ctx, entity);
	}

	@Override
	public AbsoluteFormQueryPlan clone(CloneContext ctx) {
		return new AbsoluteFormQueryPlan(
			query.clone(ctx),
			dateContexts,
			features.clone(ctx)
		);
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return query.isOfInterest(entity);
	}
}
