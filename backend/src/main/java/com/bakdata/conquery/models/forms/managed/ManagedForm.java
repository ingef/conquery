package com.bakdata.conquery.models.forms.managed;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.apiv1.URLBuilder;
import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedForm.FormSharedResult;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingState;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.api.ResultCSVResource;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdCollector;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
public class ManagedForm extends ManagedExecution<FormSharedResult> {
	
	/**
	 * The form that was submitted through the api.
	 */
	private Form submittedForm;
	
	/**
	 * Mapping of a result table name to a set of queries.
	 * This is required by forms that have multiple results (CSVs) as output.
	 */
	@JsonIgnore
	protected Map<String,List<ManagedQuery>> subQueries;

	/**
	 * Subqueries that are send to the workers.
	 */
	@InternalOnly
	private IdMap<ManagedExecutionId, ManagedQuery> flatSubQueries = new IdMap<>();
	

	public ManagedForm(Form submittedForm , UserId owner, DatasetId submittedDataset) {
		super(owner, submittedDataset);
		this.submittedForm = submittedForm;
	}
	


	@Override
	public void initExecutable(@NonNull Namespaces namespaces) {
		// init all subqueries
		synchronized (getExecution()) {
			subQueries = submittedForm.createSubQueries(namespaces, super.getOwner(), super.getDataset());
			subQueries.values().stream().flatMap(List::stream).forEach(mq -> mq.initExecutable(namespaces));
		}
	}
	
	@Override
	public void start() {
		synchronized (getExecution()) {
			subQueries.values().stream().flatMap(List::stream).forEach(flatSubQueries::add);
		}
		flatSubQueries.values().forEach(ManagedQuery::start);
		super.start();
	}

	@Override
	public Set<NamespacedId> getUsedNamespacedIds() {
		NamespacedIdCollector collector = new NamespacedIdCollector();

		for( Map.Entry<String, List<ManagedQuery>> entry : subQueries.entrySet()) {
			for(ManagedQuery subquery : entry.getValue()) {
				subquery.getQuery().visit(collector);
			}
		}

		return collector.getIds();
	}

	// Executed on Worker
	@Override
	public Map<ManagedExecutionId,QueryPlan> createQueryPlans(QueryPlanContext context) {
		synchronized (getExecution()) {			
			Map<ManagedExecutionId,QueryPlan> plans = new HashMap<>();
			for( ManagedQuery subQuery : flatSubQueries.values()) {
				plans.putAll(subQuery.createQueryPlans(context));
			}
			return plans;
		}
	}

	/**
	 * Distribute the result to a sub query.
	 */
	@Override
	public void addResult(@NonNull MetaStorage storage, FormSharedResult result) {
		ManagedExecutionId subquery = result.getSubqueryId();
		if(result.getError().isPresent()) {
			fail(storage, result.getError().get());
			return;			
		}
		ManagedQuery subQuery = flatSubQueries.get(subquery);
		subQuery.addResult(storage, result);
		switch(subQuery.getState()) {
			case DONE:
				if(allSubQueriesDone()) {
					finish(storage, ExecutionState.DONE);
				}
				break;
			case FAILED:
				// Fail the whole execution if a subquery fails
				fail(storage, result.getError().orElseThrow(
						() -> new IllegalStateException(String.format("Query [%s] failed but no error was set.",getId()))
					)
				);
				break;
			case CANCELED:
				// Ideally sub queries can not be canceled by a user, so do nothing
			case NEW:
			case RUNNING:
			default:
				break;
			
		}
		
	}


	private boolean allSubQueriesDone() {
		synchronized (getExecution()) {			
			for (ManagedQuery q : flatSubQueries.values()) {
				if (!q.getState().equals(ExecutionState.DONE)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public FormSharedResult getInitializedShardResult(Entry<ManagedExecutionId, QueryPlan> entry) {
		FormSharedResult result = new FormSharedResult();
		result.setQueryId(getId());
		if(entry != null) {
			result.setSubqueryId(entry.getKey());			
		}
		return result;
	}
	
	@Data
	@CPSType(id = "FORM_SHARD_RESULT", base = ShardResult.class)
	@EqualsAndHashCode(callSuper = true)
	public static class FormSharedResult extends ShardResult {
		private ManagedExecutionId subqueryId;
	}

	@Override
	public Set<Namespace> getRequiredNamespaces() {
		return flatSubQueries.values().stream()
			.map(ManagedQuery::getRequiredNamespaces)
			.flatMap(Set::stream)
			.collect(Collectors.toSet());
	}

	@Override
	public QueryDescription getSubmitted() {
		return submittedForm;
	}



	@Override
	public StreamingOutput getResult(IdMappingState mappingState, PrintSettings settings, Charset charset, String lineSeparator) {
		if(subQueries.size() != 1) {
			// Get the query, only if there is only one query set in the whole execution
			throw new UnsupportedOperationException("Can't return the result query of a multi query form");
		}
		return ResultCSVResource.resultAsStreamingOutput(this.getId(), settings, subQueries.values().iterator().next(), mappingState, charset, lineSeparator);
	}
	
	@Override
	protected void setAdditionalFieldsForStatusWithColumnDescription(@NonNull MetaStorage storage, URLBuilder url, User user, ExecutionStatus status) {
		super.setAdditionalFieldsForStatusWithColumnDescription(storage, url, user, status);
		// Set the ColumnDescription if the Form only consits of a single subquery
		if(subQueries == null) {
			// If subqueries was not set the Execution was not initialized
			this.initExecutable(storage.getNamespaces());
		}
		if(subQueries.size() != 1) {
			// The sub-query size might also be zero if the backend just delegates the form further to another backend. Forms with more subqueries are not yet supported
			log.trace("Column description is not generated for {} ({} from Form {}), because the form does not consits of a single subquery. Subquery size was {}.", subQueries.size(),
				this.getClass().getSimpleName(), getId(), getSubmitted().getClass().getSimpleName());
			return;
		}
		List<ManagedQuery> subQuery = subQueries.entrySet().iterator().next().getValue();
		if(subQuery.isEmpty()) {
			log.warn("The {} ({} from Form {}) does not have any subqueries after initialization. Not creating a column description.",
				this.getClass().getSimpleName(),
				getId(),
				getSubmitted().getClass().getSimpleName());
			return;
		}
		status.setColumnDescriptions(subQuery.get(0).generateColumnDescriptions());
	}


	@Override
	protected URL getDownloadURL(URLBuilder url) {
		return url.set(ResourceConstants.DATASET, dataset.getName()).set(ResourceConstants.QUERY, getId().toString())
			.to(ResultCSVResource.GET_CSV_PATH).get();
	}
}
