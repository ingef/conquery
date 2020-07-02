package com.bakdata.conquery.models.query.queryplan;

import java.util.Collection;
import java.util.stream.Stream;

import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryJob;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;

public interface QueryPlan {

	QueryPlan clone(CloneContext ctx);

	default Stream<QueryJob> executeOn(QueryExecutionContext context, Collection<Entity> entities, ShardResult result) {
		return entities
			.stream()
			.filter(this::isOfInterest)
			.map(entity -> new QueryJob(context, this, entity));
	}
	
	EntityResult execute(QueryExecutionContext ctx, Entity entity);

	boolean isOfInterest(Entity entity);
}