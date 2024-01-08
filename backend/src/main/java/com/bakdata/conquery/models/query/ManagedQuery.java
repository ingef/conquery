package com.bakdata.conquery.models.query;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.execution.ExecutionStatus;
import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.apiv1.query.EditorQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.apiv1.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.InternalExecution;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.CancelQuery;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteQuery;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.util.QueryUtils;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@ToString(callSuper = true)
@Slf4j
@CPSType(base = ManagedExecution.class, id = "MANAGED_QUERY")
public class ManagedQuery extends ManagedExecution implements EditorQuery, SingleTableResult, InternalExecution<ShardResult> {

	// Needs to be resolved externally before being executed
	private Query query;
	/**
	 * The number of contained entities the last time this query was executed.
	 */
	private Long lastResultCount;

	@JsonIgnore
	private transient Set<WorkerId> involvedWorkers;
	@JsonIgnore
	private transient List<ColumnDescriptor> columnDescriptions;


	protected ManagedQuery(@JacksonInject(useInput = OptBoolean.FALSE) MetaStorage storage) {
		super(storage);
	}

	public ManagedQuery(Query query, User owner, Dataset submittedDataset, MetaStorage storage) {
		super(owner, submittedDataset, storage);
		this.query = query;
	}

	@Override
	protected void doInitExecutable() {
		query.resolve(new QueryResolveContext(getNamespace(), getConfig(), getStorage(), null));
	}

	@Override
	public void addResult(ShardResult result) {
		log.debug("Received Result[size={}] for Query[{}]", result.getResults().size(), result.getQueryId());

		log.trace("Received Result\n{}", result.getResults());

		if (result.getError().isPresent()) {
			fail(result.getError().get());
			return;
		}

		involvedWorkers.remove(result.getWorkerId());

		getNamespace().getExecutionManager().addQueryResult(this, result.getResults());

		if (involvedWorkers.isEmpty() && getState() == ExecutionState.RUNNING) {
			finish(ExecutionState.DONE);
		}
	}

	@Override
	protected void finish(ExecutionState executionState) {
		lastResultCount = query.countResults(streamResults());

		super.finish(executionState);
	}

	public Stream<EntityResult> streamResults() {
		return getNamespace().getExecutionManager().streamQueryResults(this);
	}

	@Override
	public long resultRowCount() {
		if (lastResultCount == null) {
			throw new IllegalStateException("Result row count is unknown, because the query has not yet finished.");
		}
		return lastResultCount;
	}

	@Override
	public void start() {
		super.start();
		involvedWorkers = Collections.synchronizedSet(getNamespace().getWorkerHandler().getWorkers().stream()
																	.map(WorkerInformation::getId)
																	.collect(Collectors.toSet()));
	}

	@Override
	public void setStatusBase(@NonNull Subject subject, @NonNull ExecutionStatus status) {
		super.setStatusBase(subject, status);
		enrichStatusBase(status);
	}

	protected void setAdditionalFieldsForStatusWithColumnDescription(Subject subject, FullExecutionStatus status) {
		if (columnDescriptions == null) {
			columnDescriptions = generateColumnDescriptions(isInitialized(), getNamespace(), getConfig());
		}
		status.setColumnDescriptions(columnDescriptions);
	}

	@JsonIgnore
	public List<ResultInfo> getResultInfos() {
		return query.getResultInfos();
	}

	@Override
	public void reset() {
		super.reset();
		getNamespace().getExecutionManager().clearQueryResults(this);
	}

	@Override
	public void cancel() {
		log.debug("Sending cancel message to all workers.");

		getNamespace().getWorkerHandler().sendToAll(new CancelQuery(getId()));
	}

	@Override
	@JsonIgnore
	public QueryDescription getSubmitted() {
		return query;
	}

	/**
	 * Creates a default label based on the submitted {@link QueryDescription}.
	 * The Label is customized by mentioning that a description contained a
	 * {@link CQExternal}, {@link CQReusedQuery} or {@link CQConcept}, in this order.
	 * In case of one ore more {@link CQConcept} the distinct labels of the concepts are chosen
	 * and concatinated until a length of MAX_CONCEPT_LABEL_CONCAT_LENGTH is reached.
	 * All further labels are dropped.
	 */
	@Override
	protected String makeDefaultLabel(PrintSettings cfg) {
		return QueryUtils.makeQueryLabel(query, cfg, getId());
	}

	@Override
	public WorkerMessage createExecutionMessage() {
		return new ExecuteQuery(getId(), getQuery());
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		query.visit(visitor);
	}

	public DistributedNamespace getNamespace() {
		return (DistributedNamespace) super.getNamespace();
	}

}
