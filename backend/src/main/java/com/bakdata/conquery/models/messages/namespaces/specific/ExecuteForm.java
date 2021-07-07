package com.bakdata.conquery.models.messages.namespaces.specific;

import static com.bakdata.conquery.models.error.ConqueryError.asConqueryError;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryExecutor;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.results.FormShardResult;
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
@CPSType(id = "EXECUTE_FORM", base = NamespacedMessage.class)
@Getter
@Setter
@ToString(callSuper = true)
@AllArgsConstructor
public class ExecuteForm extends WorkerMessage {

	private final ManagedExecutionId formId;

	private final Map<ManagedExecutionId, Query> queries;

	private FormShardResult createResult(Worker worker, ManagedExecutionId subQueryId) {
		return new FormShardResult(
				getFormId(),
				subQueryId,
				worker.getInfo().getId()
		);
	}

	@Override
	public void react(Worker worker) throws Exception {

		log.info("Started Form {}", formId);

		// Execution might have been cancelled before so we uncancel it here.
		final QueryExecutor queryExecutor = worker.getQueryExecutor();

		queryExecutor.unsetQueryCancelled(formId);


		Map<ManagedExecutionId, QueryPlan<?>> plans = null;
		// Generate query plans for this execution. For ManagedQueries this is only one plan.
		// For ManagedForms there might be multiple plans, which originate from ManagedQueries.
		// The results are send directly to these ManagesQueries
		try {
			final QueryPlanContext queryPlanContext = new QueryPlanContext(worker);

			plans = queries.entrySet().stream()
						   .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().createQueryPlan(queryPlanContext)));

		}
		catch (Exception e) {
			ConqueryError err = asConqueryError(e);
			log.warn("Failed to create query plans for {}.", formId, err);
			ShardResult result = createResult(worker, null);

			queryExecutor.sendFailureToManagerNode(result, err);
			return;
		}

		// Execute all plans.
		for (Entry<ManagedExecutionId, QueryPlan<?>> entry : plans.entrySet()) {
			ShardResult result = createResult(worker, entry.getKey());

			final QueryExecutionContext subQueryContext = new QueryExecutionContext(formId, queryExecutor, worker.getStorage(), worker.getBucketManager());

			if (!queryExecutor.execute(entry.getValue(), subQueryContext, result)) {
				return;
			}
		}
	}


}
