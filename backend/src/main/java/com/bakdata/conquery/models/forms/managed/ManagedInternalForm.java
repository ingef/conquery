package com.bakdata.conquery.models.forms.managed;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.FullExecutionStatus;
import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.apiv1.forms.InternalForm;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.InternalExecution;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteForm;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.FormShardResult;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Execution type for simple forms, that are completely executed within Conquery and produce a single table as result.
 */
@Slf4j
@CPSType(base = ManagedExecution.class, id = "INTERNAL_FORM")
@Getter
public class ManagedInternalForm<F extends Form & InternalForm> extends ManagedForm<F> implements SingleTableResult, InternalExecution<FormShardResult> {


	/**
	 * Mapping of a result table name to a set of queries.
	 * This is required by forms that have multiple results (CSVs) as output.
	 */
	@JsonIgnore
	private Map<String, ManagedQuery> subQueries;

	/**
	 * Subqueries that are send to the workers.
	 */
	@JsonIgnore
	@EqualsAndHashCode.Exclude
	private final IdMap<ManagedExecutionId, ManagedQuery> flatSubQueries = new IdMap<>();

	public ManagedInternalForm(@JacksonInject(useInput = OptBoolean.FALSE) MetaStorage storage) {
		super(storage);
	}

	public ManagedInternalForm(F form, User user, Dataset submittedDataset, MetaStorage storage) {
		super(form, user, submittedDataset, storage);
	}

	@Override
	public void doInitExecutable() {
		// Convert sub queries to sub executions
		getSubmittedForm().resolve(new QueryResolveContext(getNamespace(), getConfig(), getStorage(), null));
		subQueries = createSubExecutions();

		// Initialize sub executions
		subQueries.values().forEach(mq -> mq.initExecutable(getNamespace(), getConfig()));
	}

	@NotNull
	private Map<String, ManagedQuery> createSubExecutions() {
		return getSubmittedForm().createSubQueries()
								 .entrySet()
								 .stream().collect(Collectors.toMap(
						e -> e.getKey(),
						e -> e.getValue().toManagedExecution(getOwner(), getDataset(), getStorage())

				));
	}


	@Override
	public void start() {
		synchronized (this) {
			subQueries.values().stream().forEach(flatSubQueries::add);
		}
		flatSubQueries.values().forEach(ManagedQuery::start);
		super.start();
	}

	@Override
	public List<ColumnDescriptor> generateColumnDescriptions() {
		return subQueries.values().iterator().next().generateColumnDescriptions();
	}


	@Override
	protected void setAdditionalFieldsForStatusWithColumnDescription(Subject subject, FullExecutionStatus status) {
		super.setAdditionalFieldsForStatusWithColumnDescription(subject, status);
		// Set the ColumnDescription if the Form only consits of a single subquery
		if (subQueries == null) {
			// If subqueries was not set the Execution was not initialized, do it manually
			subQueries = createSubExecutions();
		}
		if (subQueries.size() != 1) {
			// The sub-query size might also be zero if the backend just delegates the form further to another backend. Forms with more subqueries are not yet supported
			log.trace("Column description is not generated for {} ({} from Form {}), because the form does not consits of a single subquery. Subquery size was {}.", subQueries
							  .size(),
					  this.getClass().getSimpleName(), getId(), getSubmitted().getClass().getSimpleName()
			);
			return;
		}
		ManagedQuery subQuery = subQueries.entrySet().iterator().next().getValue();
		status.setColumnDescriptions(subQuery.generateColumnDescriptions());
	}

	@Override
	@JsonIgnore
	public List<ResultInfo> getResultInfos() {
		if (subQueries.size() != 1) {
			throw new UnsupportedOperationException("Cannot gather result info when multiple tables are generated");
		}
		return subQueries.values().iterator().next().getResultInfos();
	}

	@Override
	public Stream<EntityResult> streamResults() {
		if (subQueries.size() != 1) {
			// Get the query, only if there is only one query set in the whole execution
			throw new UnsupportedOperationException("Cannot return the result query of a multi query form");
		}
		return subQueries.values().iterator().next().streamResults();
	}

	@Override
	public long resultRowCount() {
		if (subQueries.size() != 1) {
			// Get the query, only if there is only one query set in the whole execution
			throw new UnsupportedOperationException("Cannot return the result query of a multi query form");
		}
		return subQueries.values().iterator().next().resultRowCount();
	}

	@Override
	public WorkerMessage createExecutionMessage() {
		return new ExecuteForm(getId(), flatSubQueries.entrySet().stream()
													  .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getQuery())));
	}

	/**
	 * Distribute the result to a sub query.
	 */
	@Override
	public void addResult(FormShardResult result) {
		if (result.getError().isPresent()) {
			fail(result.getError().get());
			return;
		}

		ManagedExecutionId subQueryId = result.getSubQueryId();

		ManagedQuery subQuery = flatSubQueries.get(subQueryId);
		subQuery.addResult(result);

		switch (subQuery.getState()) {
			case DONE -> {
				if (allSubQueriesDone()) {
					finish(ExecutionState.DONE);
				}
			}
			// Fail the whole execution if a subquery fails
			case FAILED -> {
				fail(
						result.getError().orElseThrow(
								() -> new IllegalStateException(String.format("Query [%s] failed but no error was set.", getId()))
						)
				);
			}

			default -> {
			}
		}

	}


	private boolean allSubQueriesDone() {
		synchronized (this) {
			return flatSubQueries.values().stream().allMatch(q -> q.getState().equals(ExecutionState.DONE));
		}
	}
}
