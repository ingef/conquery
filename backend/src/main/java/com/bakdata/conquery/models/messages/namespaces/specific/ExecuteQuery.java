package com.bakdata.conquery.models.messages.namespaces.specific;

import static com.bakdata.conquery.models.error.ConqueryError.asConqueryError;

import java.util.Set;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryExecutor;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Worker;
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
public class ExecuteQuery extends WorkerMessage {

	private final ManagedExecutionId id;

	private final Query query;

	@Override
	public void react(Worker worker) throws Exception {
		final ManagedExecutionId executionId = id;

		log.info("Started {} {}", query.getClass().getSimpleName(), executionId);

		// Execution might have been cancelled before so we uncancel it here.
		final QueryExecutor queryExecutor = worker.getQueryExecutor();

		queryExecutor.unsetQueryCancelled(executionId);

		final ShardResult result = createShardResult(worker);
		QueryPlanContext queryPlanContext = new QueryPlanContext(worker);

		// Before we start the query, we create it once to test if it will succeed before creating it multiple times for evaluation per core.
		try {
			query.createQueryPlan(queryPlanContext);
		}
		catch (Exception e) {
			ConqueryError err = asConqueryError(e);
			log.warn("Failed to create query plans for {}.", executionId, err);
			queryExecutor.sendFailureToManagerNode(result, err);
			return;
		}

		final QueryExecutionContext executionContext = new QueryExecutionContext(executionId, queryExecutor, worker.getStorage(), worker.getBucketManager());


		final Set<Entity> entities = query.collectRequiredEntities(executionContext).resolve(worker.getBucketManager());


		queryExecutor.execute(query, executionContext, result, entities);
	}

	private ShardResult createShardResult(Worker worker) {
		final ShardResult result = new ShardResult(id, worker.getInfo().getId());

		return result;
	}

}
