package com.bakdata.conquery.models.query.results;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.query.DistributedExecutionManager;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
@CPSType(id = "SHARD_RESULT", base = NamespacedMessage.class)
@Getter
@Setter
@Slf4j
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor(onConstructor_ = {@JsonCreator})
public class ShardResult  extends NamespaceMessage {


	@ToString.Include
	private ManagedExecutionId queryId;

	@ToString.Include
	private WorkerId workerId;

	private List<EntityResult> results = null;

	@ToString.Include
	private LocalDateTime startTime = LocalDateTime.now();

	@ToString.Include
	private LocalDateTime finishTime;

	private Optional<ConqueryError> error = Optional.empty();


	public ShardResult(ManagedExecutionId queryId, WorkerId workerId) {
		this.queryId = queryId;
		this.workerId = workerId;
	}

	public synchronized void finish(@NonNull List<EntityResult> results, Optional<Throwable> maybeError, Worker worker) {
		if (worker.getQueryExecutor().isCancelled(getQueryId())) {
			// Query is done so we no longer need the cancellation entry.
			worker.getQueryExecutor().unsetQueryCancelled(getQueryId());
			return;
		}

		finishTime = LocalDateTime.now();

		if (maybeError.isPresent()) {
			log.warn("FAILED Query[{}] within {}", queryId, Duration.between(startTime, finishTime), maybeError.get());

			setError(maybeError.map(ConqueryError::asConqueryError));
		}
		else {
			log.info("FINISHED Query[{}] with {} results within {}", queryId, results.size(), Duration.between(startTime, finishTime));
		}

		this.results = results;

		log.trace("Sending collected Results\n{}", results);

		worker.send(this);
	}

	protected void addResult(DistributedExecutionManager executionManager) {
		executionManager.handleQueryResult(this, ((ManagedQuery) executionManager.getExecution(queryId)));
	}

	@Override
	public void react(DistributedNamespace context) throws Exception {
		addResult(context.getExecutionManager());
	}
}
