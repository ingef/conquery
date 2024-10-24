package com.bakdata.conquery.models.query;

import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.execution.ExecutionStatus;
import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.apiv1.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.InternalExecution;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteQuery;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.QueryUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@ToString(callSuper = true)
@Slf4j
@CPSType(base = ManagedExecution.class, id = "MANAGED_QUERY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ManagedQuery extends ManagedExecution implements SingleTableResult, InternalExecution {

	// Needs to be resolved externally before being executed
	private Query query;
	/**
	 * The number of contained entities the last time this query was executed.
	 */
	private Long lastResultCount;



	public ManagedQuery(Query query, UserId owner, DatasetId submittedDataset, MetaStorage storage, DatasetRegistry<?> datasetRegistry, ConqueryConfig config) {
		super(owner, submittedDataset, storage, datasetRegistry, config);
		this.query = query;
	}

	@Override
	protected void doInitExecutable() {
		query.resolve(new QueryResolveContext(getNamespace(), getConfig(), getMetaStorage(), null));
	}

	@Override
	public synchronized void finish(ExecutionState executionState) {
		//TODO this is not optimal with SQLExecutionService as this might fully evaluate the query.
		lastResultCount = query.countResults(streamResults(OptionalLong.empty()));

		log.debug("Finished {} with {} results", getId(), lastResultCount);

		super.finish(executionState);
	}

	public Stream<EntityResult> streamResults(OptionalLong maybeLimit) {
		final Stream<EntityResult> results = getNamespace().getExecutionManager().streamQueryResults(this);

		if(maybeLimit.isEmpty()){
			return results;
		}

		final long limit = maybeLimit.getAsLong();
		final AtomicLong consumed = new AtomicLong();

		return results.takeWhile(line -> consumed.addAndGet(line.length()) < limit);
	}

	@Override
	public synchronized long resultRowCount() {
		if (lastResultCount == null) {
			throw new IllegalStateException("Result row count is unknown, because the query has not yet finished.");
		}
		return lastResultCount;
	}

	@Override
	public void setStatusBase(@NonNull Subject subject, @NonNull ExecutionStatus status) {

		super.setStatusBase(subject, status);
		status.setNumberOfResults(lastResultCount);

		Query query = getQuery();
		status.setQueryType(query.getClass().getAnnotation(CPSType.class).id());

		if (query instanceof SecondaryIdQuery secondaryIdQuery) {
			status.setSecondaryId((secondaryIdQuery).getSecondaryId());
		}
	}

	@Override
	protected void setAdditionalFieldsForStatusWithColumnDescription(Subject subject, FullExecutionStatus status) {
		status.setColumnDescriptions(generateColumnDescriptions(isInitialized(), getConfig()));
	}

	@JsonIgnore
	public List<ResultInfo> getResultInfos() {
		Preconditions.checkState(isInitialized());
		return query.getResultInfos();
	}

	@Override
	public void cancel() {

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
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		query.visit(visitor);
	}

	@Override
	public WorkerMessage createExecutionMessage() {
		Preconditions.checkState(isInitialized(), "Was not initialized");
		return new ExecuteQuery(getId(), getQuery());
	}
}
