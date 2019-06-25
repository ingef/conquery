package com.bakdata.conquery.models.query;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.ArrayUtils;

import com.bakdata.conquery.apiv1.URLBuilder;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.concept.ResultInfo;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.FailedEntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@Slf4j
@CPSType(base = ManagedExecution.class, id = "MANAGED_QUERY")
public class ManagedQuery extends ManagedExecution {

	private IQuery query;
	@NotNull
	private String[] tags = ArrayUtils.EMPTY_STRING_ARRAY;
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
	private transient int executingThreads;
	@JsonIgnore
	private transient List<EntityResult> results = new ArrayList<>();

	public ManagedQuery(IQuery query, Namespace namespace, UserId owner) {
		super(namespace, owner);
		this.query = query;
	}

	@Override
	public void initExecutable(Namespace namespace) {
		super.initExecutable(namespace);
		this.executingThreads = namespace.getWorkers().size();
	}

	public void addResult(ShardResult result) {
		for (EntityResult er : result.getResults()) {
			if (er.isFailed() && state == ExecutionState.RUNNING) {
				fail();
				FailedEntityResult failed = er.asFailed();
				log.error("Failed query {} at least for the entity {} with:\n{}", queryId, failed.getEntityId(), failed.getExceptionStackTrace());
			}
		}
		synchronized (execution) {
			executingThreads--;
			results.addAll(result.getResults());
			if (executingThreads == 0 && state == ExecutionState.RUNNING) {
				finish();
			}
		}
	}

	@Override
	protected void finish() {
		lastResultCount = results.stream().flatMap(ContainedEntityResult::filterCast).count();
		super.finish();
	}

	public Stream<ContainedEntityResult> fetchContainedEntityResult() {
		return results.stream().flatMap(ContainedEntityResult::filterCast);
	}

	@JsonIgnore
	public List<ResultInfo> getResultInfos(PrintSettings config) {
		return query.collectResultInfos(config);
	}
	
	@Override
	public ExecutionStatus buildStatus(URLBuilder url) {
		ExecutionStatus status = super.buildStatus(url);
		status.setTags(tags);
		status.setQuery(query);
		Long numberOfResults = Long.valueOf(fetchContainedEntityResult().count());
		status.setNumberOfResults(numberOfResults > 0 ? numberOfResults : lastResultCount);
		status.setShared(shared);
		return status;
	}
	
	@Override
	public ManagedQuery toResultQuery() {
		return this;
	}
}
