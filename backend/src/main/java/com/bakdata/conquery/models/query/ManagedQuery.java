package com.bakdata.conquery.models.query;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.concept.ResultInfo;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.FailedEntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.util.concurrent.Uninterruptibles;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Getter
@Setter
@ToString
@Slf4j
public class ManagedQuery extends IdentifiableImpl<ManagedQueryId> {

	private DatasetId dataset;
	private UUID queryId = UUID.randomUUID();
	@NotEmpty
	private String label = queryId.toString();
	private IQuery query;
	private LocalDateTime creationTime = LocalDateTime.now();
	@NotNull
	private String[] tags = new String[0];
	@Nullable
	private UserId owner;
	private boolean shared = false;
	/**
	 * The number of contained entities the last time this query was executed.
	 *
	 * @param lastResultCount the new count for JACKSON
	 * @returns the number of contained entities
	 */
	private long lastResultCount;
	//we don't want to store or send query results or other result metadata
	@JsonIgnore
	private QueryStatus status = QueryStatus.RUNNING;
	@JsonIgnore
	private int executingThreads;
	@JsonIgnore
	private CountDownLatch execution;
	@JsonIgnore
	private LocalDateTime startTime = LocalDateTime.now();
	@JsonIgnore
	private LocalDateTime finishTime;
	@JsonIgnore
	private List<EntityResult> results = new ArrayList<>();
	@JsonIgnore
	private Namespace namespace;

	public ManagedQuery(IQuery query, Namespace namespace, UserId owner) {
		this.query = query;
		this.owner = owner;
		initExecutable(namespace);
	}

	public void initExecutable(Namespace namespace) {
		this.namespace = namespace;
		this.executingThreads = namespace.getWorkers().size();
		this.execution = new CountDownLatch(1);
		this.dataset = namespace.getStorage().getDataset().getId();
	}

	@Override
	public ManagedQueryId createId() {
		return new ManagedQueryId(dataset, queryId);
	}

	public void addResult(ShardResult result) {
		for (EntityResult er : result.getResults()) {
			if (er.isFailed() && status == QueryStatus.RUNNING) {
				synchronized (execution) {
					status = QueryStatus.FAILED;
					finishTime = LocalDateTime.now();
					execution.countDown();
				}
				FailedEntityResult failed = er.asFailed();
				log.error("Failed query {} at least for the entity {} with:\n{}", queryId, failed.getEntityId(), failed.getExceptionStackTrace());
			}
		}
		synchronized (execution) {
			executingThreads--;
			results.addAll(result.getResults());
			if (executingThreads == 0 && status == QueryStatus.RUNNING) {
				finish();
			}
		}
	}



	private void finish() {
		finishTime = LocalDateTime.now();
		status = QueryStatus.DONE;
		lastResultCount = results.stream().flatMap(ContainedEntityResult::filterCast).count();
		execution.countDown();
		try {
			namespace.getStorage().getMetaStorage().updateQuery(this);
		}
		catch (JSONException e) {
			log.error("Failed to store query after finishing: " + this, e);
		}
		log.info("Finished query {} within {}", queryId, Duration.between(startTime, finishTime));
	}

	public Stream<ContainedEntityResult> fetchContainedEntityResult() {
		return results.stream().flatMap(ContainedEntityResult::filterCast);
	}

	public void awaitDone(int time, TimeUnit unit) {
		Uninterruptibles.awaitUninterruptibly(execution, time, unit);
	}

	@JsonIgnore
	public List<ResultInfo> getResultInfos() {
		return query.collectResultInfos();
	}
}
