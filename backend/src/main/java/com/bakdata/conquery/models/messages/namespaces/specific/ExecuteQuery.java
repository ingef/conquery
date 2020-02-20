package com.bakdata.conquery.models.messages.namespaces.specific;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map.Entry;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Worker;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Send message to worker to execute {@code query} on the workers associated entities.
 */
@CPSType(id="EXECUTE_QUERY", base=NamespacedMessage.class)
@AllArgsConstructor @NoArgsConstructor @Getter @Setter @ToString(callSuper=true)
public class ExecuteQuery extends WorkerMessage {

	private ManagedExecution execution;

	@Override
	public void react(Worker context) throws Exception {
		for(Entry<ManagedExecutionId, QueryPlan> entry : execution.createQueryPlans(new QueryPlanContext(context)).entrySet()) {
			execution.start();
			try {
				ShardResult result = context.getQueryExecutor().execute(new QueryExecutionContext(context.getStorage()), entry);
				result.getFuture().addListener(()->result.send(context), MoreExecutors.directExecutor());
			} catch(Exception e) {
				ShardResult result = new ShardResult();
				result.setFinishTime(LocalDateTime.now());
				result.setQueryId(execution.getId());
				result.setResults(Collections.singletonList(EntityResult.failed(-1, e)));
				context.send(new CollectQueryResult(result));
			}
		}
	}
}
