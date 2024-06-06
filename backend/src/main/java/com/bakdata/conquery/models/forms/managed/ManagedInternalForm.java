package com.bakdata.conquery.models.forms.managed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.apiv1.forms.InternalForm;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.InternalExecution;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteForm;
import com.bakdata.conquery.models.query.*;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.FormShardResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Execution type for simple forms, that are completely executed within Conquery and produce a single table as result.
 */
@Slf4j
@CPSType(base = ManagedExecution.class, id = "INTERNAL_FORM")
@Getter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ManagedInternalForm<F extends Form & InternalForm> extends ManagedForm<F> implements SingleTableResult, InternalExecution<FormShardResult> {


	/**
	 * Mapping of a result table name to a set of queries.
	 * This is required by forms that have multiple results (CSVs) as output.
	 */
	// TODO put this in the ExecutionManager
	@EqualsAndHashCode.Exclude
	private Map<String, ManagedExecutionId> subQueries;

	@JsonIgnore
	private Map<String, ManagedQuery> initializedSubQueries;

	public ManagedInternalForm(F form, UserId user, Dataset submittedDataset) {
		super(form, user, submittedDataset);
	}

	@Nullable
	public ManagedQuery getSubQuery(ManagedExecutionId subQueryId) {
		return (ManagedQuery) getMetaStorage().getExecution(subQueryId);
	}

	@Override
	public void doInitExecutable(Namespace namespace) {
		// Convert sub queries to sub executions
		getSubmitted().resolve(new QueryResolveContext(namespace, getConfig(), getMetaStorage(), null));

		if (subQueries == null || subQueries.isEmpty()) {
			// Only create sub executions if init was never executed
			subQueries = createSubExecutions();
		}

		// Initialize sub executions
		final Map<String, ManagedQuery> map = new HashMap<>(subQueries.size());
		subQueries.forEach((k,v) -> {

			ManagedExecution execution = v.resolve();
			execution.initExecutable(namespace, getConfig());
			map.put(k, (ManagedQuery) execution);
		});
		initializedSubQueries = map;
	}

	@NotNull
	private Map<String, ManagedExecutionId> createSubExecutions() {
		return getSubmitted().createSubQueries()
							 .entrySet()
							 .stream().collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> {
							ManagedQuery managedExecution = e.getValue().toManagedExecution(getOwner(), getDataset().resolve(), getMetaStorage());
							managedExecution.setSystem(true);
							getMetaStorage().updateExecution(managedExecution);
							return managedExecution.getId();
						}

				));
	}


	@Override
	public void start() {
		initializedSubQueries.values().forEach(ManagedQuery::start);
		super.start();
	}

	@Override
	public List<ColumnDescriptor> generateColumnDescriptions(boolean isInitialized, ConqueryConfig config, Namespace namespace) {
		return ((ManagedQuery) subQueries.values().iterator().next().resolve()).generateColumnDescriptions(isInitialized, config, namespace);
	}


	protected void setAdditionalFieldsForStatusWithColumnDescription(Subject subject, FullExecutionStatus status, Namespace namespace) {
		// Set the ColumnDescription if the Form only consists of a single subquery
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
		ManagedQuery subQuery = (ManagedQuery) subQueries.entrySet().iterator().next().getValue().resolve();
		status.setColumnDescriptions(subQuery.generateColumnDescriptions(isInitialized(), getConfig(), namespace));
	}

	@Override
	public void cancel() {
		subQueries.values().stream().map(Id::resolve).map(ManagedQuery.class::cast).forEach(ManagedQuery::cancel);
	}

	@Override
	@JsonIgnore
	public List<ResultInfo> getResultInfos() {
		if (subQueries.size() != 1) {
			throw new UnsupportedOperationException("Cannot gather result info when multiple tables are generated");
		}
		return ((ManagedQuery) subQueries.values().iterator().next().resolve()).getResultInfos();
	}

	@Override
	public Stream<EntityResult> streamResults(OptionalLong limit, ExecutionManager<?> executionManager) {
		if (subQueries.size() != 1) {
			// Get the query, only if there is only one query set in the whole execution
			throw new UnsupportedOperationException("Cannot return the result query of a multi query form");
		}
		return ((ManagedQuery) subQueries.values().iterator().next().resolve()).streamResults(limit, executionManager);
	}

	@Override
	public long resultRowCount() {
		if (subQueries.size() != 1) {
			// Get the query, only if there is only one query set in the whole execution
			throw new UnsupportedOperationException("Cannot return the result query of a multi query form");
		}
		return ((ManagedQuery) subQueries.values().iterator().next().resolve()).resultRowCount();
	}

	@Override
	public WorkerMessage createExecutionMessage() {
		return new ExecuteForm(getId(), initializedSubQueries.values().stream()
													  .collect(Collectors.toMap(ManagedQuery::getId, ManagedQuery::getQuery)));
	}


	public boolean allSubQueriesDone() {
		synchronized (this) {
			return subQueries.values().stream().map(Id::resolve).allMatch(q -> q.getState().equals(ExecutionState.DONE));
		}
	}
}
