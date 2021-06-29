package com.bakdata.conquery.models.query.results;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.specific.CollectQueryResult;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
@CPSType(id = "SHARD_RESULT", base = ShardResult.class)
@Getter
@Setter
@Slf4j
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class ShardResult {


	@ToString.Include
	private ManagedExecutionId queryId;

	@ToString.Include
	private WorkerId workerId;

	private List<EntityResult> results = new ArrayList<>();

	@ToString.Include
	private LocalDateTime startTime = LocalDateTime.now();

	@ToString.Include
	private LocalDateTime finishTime;

	private Optional<ConqueryError> error = Optional.empty();


	public ShardResult(ManagedExecutionId queryId, WorkerId workerId) {
		this.queryId = queryId;
		this.workerId = workerId;
	}

	public synchronized void finish(List<EntityResult> results, Optional<Throwable> exc, Worker worker) {
		if (worker.getQueryExecutor().isCancelled(getQueryId())) {
			// Query is done so we no longer need the cancellation entry.
			worker.getQueryExecutor().unsetQueryCancelled(getQueryId());
			return;
		}

		finishTime = LocalDateTime.now();

		if (exc.isPresent()) {
			log.info("FAILED Query[{}] with {} results within {}", queryId, this.results.size(), Duration.between(startTime, finishTime));

			setError(exc.map(ConqueryError::asConqueryError));
		}
		else {
			log.info("FINISHED Query[{}] with {} results within {}", queryId, results.size(), Duration.between(startTime, finishTime));
		}

		this.results = results;

		worker.send(new CollectQueryResult(this));
	}

}
