package com.bakdata.conquery.models.query;

import java.util.concurrent.Callable;

import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.error.ConqueryException;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QueryJob implements Callable<EntityResult> {

	private final QueryExecutionContext ctx;
	private final QueryPlan plan;
	private final Entity entity;
	
	@Override
	public EntityResult call() throws Exception {
		try {
			CloneContext cCtx = new CloneContext(ctx.getStorage());
			QueryPlan queryPlan = this.plan.clone(cCtx);
			
			return queryPlan.execute(ctx, entity);
		}
		catch (ConqueryException e) {
			// Catch known errors (where the user can possibly fix something)
			return EntityResult.failed(entity.getId(), e.getCtx());
		}
		catch (Exception e) {
			// Catch unspecified errors, log them with their id and forward them as unknown errors.
			return EntityResult.failed(entity.getId(), new ConqueryError.UnknownError(e));
		}
	}
}
