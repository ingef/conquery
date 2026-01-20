package com.bakdata.conquery.models.query.results;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import javax.annotation.Nullable;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ExecutionState;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.core.future.WriteFuture;

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
	private ManagedExecutionId executionId;

	@ToString.Include
	private WorkerId workerId;

	private List<EntityResult> results = null;

	@ToString.Include
	private LocalDateTime startTime = LocalDateTime.now();

	@ToString.Include
	private LocalDateTime finishTime;

	private ConqueryError error;


	public ShardResult(ManagedExecutionId executionId, WorkerId workerId) {
		this.executionId = executionId;
		this.workerId = workerId;
	}

	public synchronized void finish(@NonNull List<EntityResult> results, @Nullable Throwable maybeError, Worker worker) {
		if (worker.getQueryExecutor().isCancelled(getExecutionId())) {
			// Query is done so we no longer need the cancellation entry.
			worker.getQueryExecutor().unsetQueryCancelled(getExecutionId());
			return;
		}

		finishTime = LocalDateTime.now();

		if (maybeError != null) {
			log.warn("FAILED Query[{}] within {}", executionId, Duration.between(startTime, finishTime), maybeError);

			setError(ConqueryError.asConqueryError(maybeError));
		}
		else {
			log.info("FINISHED Query[{}] with {} results within {}", executionId, results.size(), Duration.between(startTime, finishTime));
		}

		this.results = results;

		log.trace("Sending collected Results for execution {}\n{}", executionId, StringUtils.truncate(results.toString(),500));

		// Wrap in try-catch to catch errors in the mina filter chain
		try {
			WriteFuture sendResult = worker.send(this);

			// Add listener to handle errors that may arise after the filter chain
			sendResult.addListener(resultWriteFurture -> {

				WriteFuture wf = (WriteFuture) resultWriteFurture;
				if (wf.isWritten()) {
					log.trace("Successfully submitted shard result for execution {}", executionId);
					return;
				}

				Throwable exception = wf.getException();

				handleTransmissionFailure(worker, exception);
			});
		} catch (Throwable throwable) {
			// Throwable because we might get an OOM if a message was too large
			handleTransmissionFailure(worker,throwable);
		}


	}

	private void handleTransmissionFailure(Worker worker, Throwable throwable) {
		log.error(
				"Failed to submit (otherwise fine) shard result for execution {}. Notifying manager", executionId, throwable
		);

		ShardResult failMessage = new ShardResult(executionId, workerId);
		failMessage.setError(ConqueryError.asConqueryError(throwable));

		WriteFuture sendFailure = worker.send(failMessage);
		sendFailure.addListener(failWriteFuture -> {
			if (((WriteFuture)failWriteFuture).isWritten()) {
				log.info("Successfully informed manager about failed shard result for execution {}", executionId);
				return;
			}
			log.error("Could not notify manager about shard result submission failure for execution {}. "
					  + "Manager probably has this execution in a dangling {} state",
					  executionId, ExecutionState.RUNNING);
		});
	}

	protected void addResult(DistributedExecutionManager executionManager) {
		executionManager.handleQueryResult(this, ((ManagedQuery) executionManager.getExecution(executionId)));
	}

	@Override
	public void react(DistributedNamespace context) throws Exception {
		addResult(context.getExecutionManager());
	}
}
