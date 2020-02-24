package com.bakdata.conquery.models.forms.managed;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.SubmittedQuery;
import com.bakdata.conquery.apiv1.URLBuilder;
import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedForm.FormSharedResult;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdCollector;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Internal runtime representation of a form query.
 */
//@NoArgsConstructor
@Getter
@Setter
@ToString
@Slf4j
@CPSType(base = ManagedExecution.class, id = "MANAGED_FORM")
public class ManagedForm extends ManagedExecution<FormSharedResult> {
	
	/**
	 * The form that was submitted through the api.
	 */
	private Form submittedForm;
	
	@JsonIgnore
	protected Map<String,List<ManagedQuery>> subQueries;

	@InternalOnly
	private Map<ManagedExecutionId,ManagedQuery> flatSubQueries;
	
	@JsonIgnore
	private transient AtomicInteger openSubQueries;
	
	public ManagedForm(MasterMetaStorage storage, Form submittedForm, UserId owner, DatasetId submittedDataset) {
		super(storage,  owner, submittedDataset);
		this.submittedForm = submittedForm;
	}
	


	@Override
	public void initExecutable(@NonNull Namespaces namespaces) {
		// init all subqueries
		subQueries = submittedForm.createSubQueries(namespaces, super.getOwner(), super.getDataset());
		flatSubQueries = subQueries.values().stream().flatMap(List::stream).collect(Collectors.toMap(ManagedQuery::getId, Function.identity()));
		flatSubQueries.values().forEach(mq -> mq.initExecutable(namespaces));
		openSubQueries = new AtomicInteger(flatSubQueries.values().size());
	}
	
	@Override
	public void start() {
		flatSubQueries.values().forEach(ManagedQuery::start);
		super.start();
	}
	
	@Override
	public ExecutionStatus buildStatus(URLBuilder url, User user) {
		ExecutionStatus status = super.buildStatus(url, user);
		// Send null here, because no usable value can be reported to the user for a form
		status.setNumberOfResults(null);
		return status;
	}

	@Override
	public Collection<ManagedQuery> toResultQuery() {
		if(subQueries.size() == 1) {
			// Get the query, only if there is only one in the whole execution
			return subQueries.values().iterator().next();
		}
		throw new UnsupportedOperationException("Can't return the result query of a multi query form");
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
		Map<ManagedExecutionId,QueryPlan> plans = new HashMap<>();
		for( ManagedQuery subQuery : flatSubQueries.values()) {
			plans.putAll(subQuery.createQueryPlans(context));
		}
		return plans;
	}

	/**
	 * Distribute the result to a sub query.
	 */
	@Override
	public void addResult(FormSharedResult result) {
		ManagedQuery subQuery = flatSubQueries.get(result.getSubqueryId());
		subQuery.addResult(result);
		switch(subQuery.getState()) {
			case CANCELED:
				break;
			case DONE:
				if(openSubQueries.decrementAndGet() == 0) {
					finish();
				}
				break;
			case FAILED:
				fail();
				break;
			case NEW:
				break;
			case RUNNING:
				break;
			default:
				break;
			
		}
		
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
	public SubmittedQuery getSubmitted() {
		return submittedForm;
	}
}
