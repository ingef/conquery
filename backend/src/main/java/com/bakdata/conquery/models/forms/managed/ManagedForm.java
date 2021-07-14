package com.bakdata.conquery.models.forms.managed;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.FullExecutionStatus;
import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.results.FormShardResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdentifiableCollector;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Internal runtime representation of a form query.
 */
@Getter
@Setter
@ToString
@Slf4j
@CPSType(base = ManagedExecution.class, id = "MANAGED_FORM")
@NoArgsConstructor
public abstract class ManagedForm extends ManagedExecution<FormShardResult> {

	/**
	 * The form that was submitted through the api.
	 */
	private Form submittedForm;

	/**
	 * Mapping of a result table name to a set of queries.
	 * This is required by forms that have multiple results (CSVs) as output.
	 */
	@JsonIgnore
	protected Map<String, List<ManagedQuery>> subQueries;

	/**
	 * Subqueries that are send to the workers.
	 */
	@InternalOnly
	private IdMap<ManagedExecutionId, ManagedQuery> flatSubQueries = new IdMap<>();


	public ManagedForm(Form submittedForm, User owner, Dataset submittedDataset) {
		super(owner, submittedDataset);
		this.submittedForm = submittedForm;
	}


	@Override
	public void doInitExecutable(@NonNull DatasetRegistry datasetRegistry, ConqueryConfig config) {
		// init all subqueries
		submittedForm.resolve(new QueryResolveContext(getDataset(), datasetRegistry, config, null));
		subQueries = submittedForm.createSubQueries(datasetRegistry, super.getOwner(), getDataset());
		subQueries.values().stream().flatMap(List::stream).forEach(mq -> mq.initExecutable(datasetRegistry, config));
	}

	@Override
	public void start() {
		synchronized (this) {
			subQueries.values().stream().flatMap(List::stream).forEach(flatSubQueries::add);
		}
		flatSubQueries.values().forEach(ManagedQuery::start);
		super.start();
	}

	@Override
	public Set<NamespacedIdentifiable<?>> getUsedNamespacedIds() {
		NamespacedIdentifiableCollector collector = new NamespacedIdentifiableCollector();

		for (Map.Entry<String, List<ManagedQuery>> entry : subQueries.entrySet()) {
			for (ManagedQuery subquery : entry.getValue()) {
				subquery.getQuery().visit(collector);
			}
		}

		return collector.getIdentifiables();
	}


	/**
	 * Distribute the result to a sub query.
	 */
	@Override
	public void addResult(@NonNull MetaStorage storage, FormShardResult result) {
		if (result.getError().isPresent()) {
			fail(storage, result.getError().get());
			return;
		}

		ManagedExecutionId subQueryId = result.getSubQueryId();

		ManagedQuery subQuery = flatSubQueries.get(subQueryId);
		subQuery.addResult(storage, result);

		switch (subQuery.getState()) {
			case DONE:
				if (allSubQueriesDone()) {
					finish(storage, ExecutionState.DONE);
				}
				break;
			case FAILED:
				// Fail the whole execution if a subquery fails
				fail(storage, result.getError().orElseThrow(
						() -> new IllegalStateException(String.format("Query [%s] failed but no error was set.", getId()))
					 )
				);
				break;
			case NEW:
			case RUNNING:
			default:
				break;

		}

	}


	private boolean allSubQueriesDone() {
		synchronized (this) {
			for (ManagedQuery q : flatSubQueries.values()) {
				if (!q.getState().equals(ExecutionState.DONE)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		submittedForm.visit(visitor);
	}

	@Override
	@JsonIgnore
	public Set<Namespace> getRequiredDatasets() {
		return flatSubQueries.values().stream()
							 .map(ManagedQuery::getRequiredDatasets)
							 .flatMap(Set::stream)
							 .collect(Collectors.toSet());
	}

	@Override
	@JsonIgnore
	public QueryDescription getSubmitted() {
		return submittedForm;
	}


	@Override
	protected void setAdditionalFieldsForStatusWithColumnDescription(@NonNull MetaStorage storage, UriBuilder url, User user, FullExecutionStatus status, DatasetRegistry datasetRegistry) {
		super.setAdditionalFieldsForStatusWithColumnDescription(storage, url, user, status, datasetRegistry);
		// Set the ColumnDescription if the Form only consits of a single subquery
		if (subQueries == null) {
			// If subqueries was not set the Execution was not initialized, do it manually
			subQueries = submittedForm.createSubQueries(datasetRegistry, super.getOwner(), super.getDataset());
		}
		if (subQueries.size() != 1) {
			// The sub-query size might also be zero if the backend just delegates the form further to another backend. Forms with more subqueries are not yet supported
			log.trace("Column description is not generated for {} ({} from Form {}), because the form does not consits of a single subquery. Subquery size was {}.", subQueries
																																											 .size(),
					  this.getClass().getSimpleName(), getId(), getSubmitted().getClass().getSimpleName()
			);
			return;
		}
		List<ManagedQuery> subQuery = subQueries.entrySet().iterator().next().getValue();
		if (subQuery.isEmpty()) {
			log.warn(
					"The {} ({} from Form {}) does not have any subqueries after initialization. Not creating a column description.",
					this.getClass().getSimpleName(),
					getId(),
					getSubmitted().getClass().getSimpleName()
			);
			return;
		}
		status.setColumnDescriptions(subQuery.get(0).generateColumnDescriptions(datasetRegistry));
	}


	@Override
	protected String makeDefaultLabel(PrintSettings cfg) {
		return getSubmittedForm().getLocalizedTypeLabel()
			   + " "
			   + getCreationTime().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", I18n.LOCALE.get()));
	}

}
