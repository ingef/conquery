package com.bakdata.conquery.models.query;

import java.util.Optional;
import java.util.function.Supplier;

import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QueryJob implements Supplier<Optional<EntityResult>> {

	private final QueryExecutionContext ctx;
	private final QueryPlan plan;
	private final Entity entity;
	
	@Override
	public Optional<EntityResult> get() {

		if(ctx.isQueryCancelled()){
			return Optional.empty();
		}

		try {
			CloneContext cCtx = new CloneContext(ctx.getStorage());
			QueryPlan queryPlan = plan.clone(cCtx);
			
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
