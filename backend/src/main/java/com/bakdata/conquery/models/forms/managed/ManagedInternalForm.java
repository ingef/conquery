package com.bakdata.conquery.models.forms.managed;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.apiv1.forms.InternalForm;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.InternalExecution;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteForm;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
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
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ManagedInternalForm<F extends Form & InternalForm> extends ManagedForm<F> implements SingleTableResult, InternalExecution {


	/**
	 * Mapping of a result table name to a set of queries.
	 * This is required by forms that have multiple results (CSVs) as output.
	 */
	@EqualsAndHashCode.Exclude
	@Getter
	private Map<String, ManagedExecutionId> subQueries;

	/**
	 * These are only here to keep the sub queries initialized between init and start.
	 * Use these only on an initialized execution, as it might be null.
	 */
	@JsonIgnore
	@Getter(AccessLevel.PROTECTED)
	private Map<String, ManagedQuery> initializedSubQueryHardRef;

	public ManagedInternalForm(F form, UserId user, DatasetId submittedDataset, MetaStorage storage, DatasetRegistry<?> datasetRegistry, ConqueryConfig config) {
		super(form, user, submittedDataset, storage, datasetRegistry, config);
	}

	@Nullable
	public ManagedQuery getSubQuery(ManagedExecutionId subQueryId) {
		if (subQueries.containsValue(subQueryId)) {
			return (ManagedQuery) getMetaStorage().getExecution(subQueryId);
		}
		return null;
	}

	protected void setAdditionalFieldsForStatusWithColumnDescription(Subject subject, FullExecutionStatus status) {
		// Set the ColumnDescription if the Form only consits of a single subquery
		if (subQueries == null) {
			// If subqueries was not set the Execution was not initialized, do it manually
			doInitExecutable();
		}
		if (subQueries.size() != 1) {
			// The sub-query size might also be zero if the backend just delegates the form further to another backend. Forms with more subqueries are not yet supported
			log.trace("Column description is not generated for {} ({} from Form {}), because the form does not consits of a single subquery. Subquery size was {}.",
					  subQueries.size(),
					  this.getClass().getSimpleName(),
					  getId(),
					  getSubmitted().getClass().getSimpleName()
			);
			return;
		}
		ManagedQuery subQuery = (ManagedQuery) subQueries.entrySet().iterator().next().getValue().resolve();
		status.setColumnDescriptions(subQuery.generateColumnDescriptions(isInitialized(), getConfig()));
	}

	@Override
	public void doInitExecutable() {
		// Convert sub queries to sub executions
		getSubmitted().resolve(new QueryResolveContext(getNamespace(), getConfig(), getMetaStorage(), null));
		Map<String, ManagedQuery> subQueries = resolveOrCreateSubExecutions();

		// Initialize sub executions and persist them
		subQueries.values().forEach(mq -> {
			mq.initExecutable();
			getMetaStorage().updateExecution(mq);
		});

		// Create hard ref to couple the object lifetime of the subqueries to this object
		initializedSubQueryHardRef = subQueries;

		this.subQueries = subQueries.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getId()));
	}

	@NotNull
	private Map<String, ManagedQuery> resolveOrCreateSubExecutions() {
		if (subQueries != null && !subQueries.isEmpty()) {
			// This execution was already executed once, to we resolve the corresponding subqueries
			return subQueries.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (ManagedQuery) e.getValue().resolve()));
		}

		return getSubmitted().createSubQueries()
							 .entrySet()
							 .stream()
							 .collect(Collectors.toMap(
									 Map.Entry::getKey,
									 e -> e.getValue().toManagedExecution(getOwner(), getDataset(), getMetaStorage(), getDatasetRegistry(), getConfig())
							 ));
	}

	@Override
	public void start() {
		initializedSubQueryHardRef.values().forEach(ManagedExecution::start);
		super.start();
	}

	@Override
	public List<ColumnDescriptor> generateColumnDescriptions(boolean isInitialized, ConqueryConfig config) {
		return ((ManagedQuery) subQueries.values().iterator().next().resolve()).generateColumnDescriptions(isInitialized, config);
	}

	@Override
	@JsonIgnore
	public List<ResultInfo> collectResultInfos() {
		if (subQueries.size() != 1) {
			throw new UnsupportedOperationException("Cannot gather result info when multiple tables are generated");
		}
		return ((ManagedQuery) subQueries.values().iterator().next().resolve()).collectResultInfos();
	}

	@Override
	@JsonIgnore
	public List<ResultInfo> getResultInfos() {
		ExecutionManager.InternalExecutionInfo executionInfo = getNamespace().getExecutionManager().getExecutionInfo(getId());
		return executionInfo.getResultInfos();
	}

	@Override
	public Stream<EntityResult> streamResults(OptionalLong limit) {
		if (subQueries.size() != 1) {
			// Get the query, only if there is only one query set in the whole execution
			throw new UnsupportedOperationException("Cannot return the result query of a multi query form");
		}
		return ((ManagedQuery) subQueries.values().iterator().next().resolve()).streamResults(limit);
	}

	@Override
	public long resultRowCount() {
		if (subQueries.size() != 1) {
			// Get the query, only if there is only one query set in the whole execution
			throw new UnsupportedOperationException("Cannot return the result query of a multi query form");
		}
		return ((ManagedQuery) subQueries.values().iterator().next().resolve()).resultRowCount();
	}

	public boolean allSubQueriesDone() {
		synchronized (this) {
			return subQueries.values().stream().map(ManagedExecutionId::resolve).allMatch(q -> q.getState().equals(ExecutionState.DONE));
		}
	}

	protected Map<String, ManagedQuery> resolvedSubQueries() {
		return subQueries.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (ManagedQuery) e.getValue().resolve()));
	}

	public ExecuteForm createExecutionMessage() {
		Preconditions.checkState(isInitialized(), "Was not initialized");
		return new ExecuteForm(getId(), initializedSubQueryHardRef.values()
																  .stream()
																  .collect(Collectors.toMap(ManagedExecution::getId, ManagedQuery::getQuery))
		);
	}

}
