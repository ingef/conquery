package com.bakdata.conquery.models.query.results;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

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
import org.apache.commons.lang3.StringUtils;

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

	public synchronized void finish(@NonNull List<EntityResult> results, Worker worker) {
		if (worker.getQueryExecutor().isCancelled(getExecutionId())) {
			// Query is done so we no longer need the cancellation entry.
			worker.getQueryExecutor().unsetQueryCancelled(getExecutionId());
			return;
		}

		finishTime = LocalDateTime.now();


		log.info("FINISHED Query[{}] with {} results within {}", executionId, results.size(), Duration.between(startTime, finishTime));

		this.results = results;

		// Truncate here because too large logs will crash/lock the process
		log.trace("Collected Results for execution {}\n{}", executionId, StringUtils.truncate(results.toString(), 1000) + " (...)");
	}
	
	protected void addResult(DistributedExecutionManager executionManager) {
		executionManager.handleQueryResult(this, ((ManagedQuery) executionManager.getExecution(executionId)));
	}

	@Override
	public void react(DistributedNamespace context) throws Exception {
		addResult(context.getExecutionManager());
	}
}
