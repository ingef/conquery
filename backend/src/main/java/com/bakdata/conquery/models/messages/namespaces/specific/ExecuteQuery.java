package com.bakdata.conquery.models.messages.namespaces.specific;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;

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

	private ManagedExecution<?> execution;

	@Override
	public void react(Worker context) throws Exception {
		Set<Entry<ManagedExecutionId, QueryPlan>> plans = null;
		// Generate query plans for this execution. For ManagedQueries this is only one plan.
		// For ManagedForms there might be multiple plans, which originate from ManagedQueries.
		// The results are send directly to these ManagesQueries
		try {
			plans = execution.createQueryPlans(new QueryPlanContext(context)).entrySet();		
		} catch (Exception e) {
			// If one of the plans can not be created (maybe due to a Id that references a non existing concept) fail the whole job.
			sendFailureToMaster(execution.getInitializedShardResult(null), execution, context, e);
		}
		// Execute all plans.
		for(Entry<ManagedExecutionId, QueryPlan> entry : plans) {
			ShardResult result = execution.getInitializedShardResult(entry);
			try {
				context.getQueryExecutor().execute(result, new QueryExecutionContext(context.getStorage()), entry);
				result.getFuture().addListener(()->result.send(context), MoreExecutors.directExecutor());
			} catch(Exception e) {
				sendFailureToMaster(result, execution, context, e);
			}
		}
	}

	private static void sendFailureToMaster(ShardResult result, ManagedExecution<?> execution, Worker context, Exception e) {
		result.setFinishTime(LocalDateTime.now());
		result.setResults(Collections.singletonList(EntityResult.failed(-1, e)));
		context.send(new CollectQueryResult(result));
	}
}
