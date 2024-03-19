package com.bakdata.conquery.models.messages.namespaces.specific;

import static com.bakdata.conquery.models.error.ConqueryError.asConqueryError;

import java.util.Set;
import java.util.UUID;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.metrics.ExecutionMetrics;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.messages.namespaces.ActionReactionMessage;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryExecutor;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Send message to worker to execute {@code query} on the workers associated entities.
 */
@Slf4j
@CPSType(id = "EXECUTE_QUERY", base = NamespacedMessage.class)
@Getter
@Setter
@ToString(callSuper = true)
@AllArgsConstructor
public class ExecuteQuery extends WorkerMessage  implements ActionReactionMessage {

	private final ManagedExecutionId id;

	private final Query query;

	// Only relevant on Manager with ActionReactionMessage
	@JsonIgnore
	private final MetaStorage storage;

	@Override
	public void react(Worker worker) throws Exception {
		final ManagedExecutionId executionId = id;

		log.info("Started {} {}", query.getClass().getSimpleName(), executionId);

		// Execution might have been cancelled before, so we uncancel it here.
		final QueryExecutor queryExecutor = worker.getQueryExecutor();

		queryExecutor.unsetQueryCancelled(executionId);

		final ShardResult result = new ShardResult(id, worker.getInfo().getId());

		// Before we start the query, we create it once to test if it will succeed before creating it multiple times for evaluation per core.
		try {
			query.createQueryPlan(new QueryPlanContext(worker, queryExecutor.getSecondaryIdSubPlanLimit()));
		}
		catch (Exception e) {
			final ConqueryError err = asConqueryError(e);
			log.warn("Failed to create query plans for {}.", executionId, err);
			queryExecutor.sendFailureToManagerNode(result, err);
			return;
		}

		final QueryExecutionContext executionContext = new QueryExecutionContext(executionId, queryExecutor, worker.getStorage(), worker.getBucketManager());

		final Set<Entity> entities = query.collectRequiredEntities(executionContext).resolve(worker.getBucketManager());

		queryExecutor.execute(query, executionContext, result, entities);
	}

	@Override
	@JsonIgnore
	public UUID getMessageId() {
		return id.getExecution();
	}

	@Override
	public void afterAllReaction() {
		final ManagedExecution execution = getStorage().getExecution(id);

		execution.finish(ExecutionState.DONE);

		// State changed to DONE or FAILED
		if (execution.getState() != ExecutionState.RUNNING) {
			final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(execution.getOwner(), getStorage()).map(Group::getName).orElse("none");

			ExecutionMetrics.getRunningQueriesCounter(primaryGroupName).dec();
			ExecutionMetrics.getQueryStateCounter(execution.getState(), primaryGroupName).inc();
			ExecutionMetrics.getQueriesTimeHistogram(primaryGroupName).update(execution.getExecutionTime().toMillis());

//			/* This log is here to prevent an NPE which could occur when no strong reference to result.getResults()
//			 existed anymore after the query finished and immediately was reset */
//			log.trace("Collected metrics for execution {}. Last result received: {}:", result.getQueryId(), result.getResults());
		}
	}
}
