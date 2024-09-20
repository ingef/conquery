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
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.InternalExecution;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
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
public class ManagedInternalForm<F extends Form & InternalForm> extends ManagedForm<F> implements SingleTableResult, InternalExecution {


	/**
	 * Mapping of a result table name to a set of queries.
	 * This is required by forms that have multiple results (CSVs) as output.
	 */
	@JsonIgnore
	@EqualsAndHashCode.Exclude
	private Map<String, ManagedQuery> subQueries;

	/**
	 * Subqueries that are sent to the workers.
	 */
	@JsonIgnore
	@EqualsAndHashCode.Exclude
	private final IdMap<ManagedExecutionId, ManagedQuery> flatSubQueries = new IdMap<>();

	public ManagedInternalForm(F form, User user, Dataset submittedDataset, MetaStorage storage) {
		super(form, user, submittedDataset, storage);
	}

	@Nullable
	public ManagedQuery getSubQuery(ManagedExecutionId subQueryId) {
		return flatSubQueries.get(subQueryId);
	}

	@Override
	public void doInitExecutable(Namespace namespace) {
		// Convert sub queries to sub executions
		getSubmitted().resolve(new QueryResolveContext(getNamespace(), getConfig(), getMetaStorage(), null));
		subQueries = createSubExecutions();

		// Initialize sub executions
		subQueries.values().forEach(mq -> mq.initExecutable(getNamespace(), getConfig()));
	}

	@NotNull
	private Map<String, ManagedQuery> createSubExecutions() {
		return getSubmitted().createSubQueries()
							 .entrySet()
							 .stream().collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> e.getValue().toManagedExecution(getOwner(), getDataset(), getMetaStorage())

				));
	}


	@Override
	public void start(ExecutionManager executionManager) {
		synchronized (this) {
			subQueries.values().forEach(flatSubQueries::add);
		}
		flatSubQueries.values().forEach(query -> query.start(executionManager));
		super.start(executionManager);
	}

	@Override
	public List<ColumnDescriptor> generateColumnDescriptions(boolean isInitialized, ConqueryConfig config) {
		return subQueries.values().iterator().next().generateColumnDescriptions(isInitialized, config);
	}


	protected void setAdditionalFieldsForStatusWithColumnDescription(Subject subject, FullExecutionStatus status, Namespace namespace) {
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
		status.setColumnDescriptions(subQuery.generateColumnDescriptions(isInitialized(), getConfig()));
	}

	@Override
	public void cancel() {
		subQueries.values().forEach(ManagedQuery::cancel);
	}

	@Override
	@JsonIgnore
	public List<ResultInfo> getResultInfos(PrintSettings printSettings) {
		if (subQueries.size() != 1) {
			throw new UnsupportedOperationException("Cannot gather result info when multiple tables are generated");
		}
		return subQueries.values().iterator().next().getResultInfos(printSettings);
	}

	@Override
	public Stream<EntityResult> streamResults(OptionalLong limit) {
		if (subQueries.size() != 1) {
			// Get the query, only if there is only one query set in the whole execution
			throw new UnsupportedOperationException("Cannot return the result query of a multi query form");
		}
		return subQueries.values().iterator().next().streamResults(limit);
	}

	@Override
	public long resultRowCount() {
		if (subQueries.size() != 1) {
			// Get the query, only if there is only one query set in the whole execution
			throw new UnsupportedOperationException("Cannot return the result query of a multi query form");
		}
		return subQueries.values().iterator().next().resultRowCount();
	}

	public boolean allSubQueriesDone(ExecutionManager executionManager) {
		synchronized (this) {
			return flatSubQueries.values().stream().allMatch(q -> q.getState(executionManager).equals(ExecutionState.DONE));
		}
	}

}
