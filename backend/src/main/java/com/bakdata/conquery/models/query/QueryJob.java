package com.bakdata.conquery.models.query;

import java.util.Optional;
import java.util.concurrent.Callable;

import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QueryJob<R extends EntityResult> implements Callable<Optional<R>>{

	private final QueryExecutionContext ctx;
	private final QueryPlan<R> plan;
	private final Entity entity;
	
	@Override
	public Optional<R> call() throws Exception {
		try {
			CloneContext cCtx = new CloneContext(ctx.getStorage());
			QueryPlan<R> queryPlan = cCtx.clone(this.plan);
			
			return queryPlan.execute(ctx, entity);
		}
		catch (ConqueryError e) {
			// Catch errors, propagate them with their id.
			throw new ConqueryError.ExecutionJobErrorWrapper(entity,e);
		}
		catch (Exception e) {
			throw new ConqueryError.ExecutionJobErrorWrapper(entity,new ConqueryError.UnknownError(e));
		}
	}
}
