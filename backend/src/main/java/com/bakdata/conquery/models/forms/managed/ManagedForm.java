package com.bakdata.conquery.models.forms.managed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.URLBuilder;
import com.bakdata.conquery.io.cps.CPSType;
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
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdCollector;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Getter;
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
	
	@JsonIgnore
	protected Map<String,List<ManagedQuery>> subQueries;

	private Map<ManagedExecutionId,ManagedQuery> flatSubQueries;
	@JsonIgnore
	private transient AtomicInteger openSubQueries;
	
	

//	@JsonIgnore
//	private transient ListenableFuture<ExecutionState> formFuture;

//	/**
//	 * Represents the mapping of result matrices with their output name to their actual queries.
//	 * The output name is later used to generate the name of the CSV file.
//	 */
//	protected Map<String,List<EntityResult>> internalQueryMapping;

	public ManagedForm(MasterMetaStorage storage, Map<String,List<ManagedQuery>> subQueries, UserId owner, DatasetId submittedDataset) {
		super(storage,  owner, submittedDataset);
		this.subQueries = subQueries;
	}

//	public void executeForm(ListeningExecutorService pool, MasterMetaStorage storage) {
//		setState(ExecutionState.RUNNING);
//		formFuture = pool.submit(() -> execute(storage));
//		Futures.addCallback(
//			formFuture,
//			new FutureCallback<ExecutionState>() {
//				@Override
//				public void onSuccess(ExecutionState result) {
//					if(result == ExecutionState.DONE) {
//						finish();
//					}
//					else {
//						fail();
//					}
//				}
//				@Override
//				public void onFailure(Throwable t) {
//					log.error("Failed executing form "+form.getId(), t);
//					fail();
//				}
//			},
//			MoreExecutors.directExecutor()
//		);
//	}
//	
//	protected ExecutionState execute(MasterMetaStorage storage) throws JSONException, IOException {
//		internalQueryMapping = form.executeQuery(
//			getNamespace().getStorage().getDataset(),
//			getNamespace().getStorage().getMetaStorage().getUser(getOwner()),
//			getNamespace().getNamespaces()
//		);
//		for(List<ManagedQuery> internalQueries : internalQueryMapping.values()) {
//			for(ManagedQuery internalQuery : internalQueries) {
//				internalQuery.awaitDone(1, TimeUnit.DAYS);
//				if(internalQuery.getState() != ExecutionState.DONE) {
//					return ExecutionState.FAILED;
//				}
//			}
//		}
//		return ExecutionState.DONE;
//	}
	
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
	public ManagedQuery toResultQuery() {
//		if(subQueries.size() == 1) {
//			// Get the query, only if there is only one in the whole execution
//			subQueries.get(0).stream().flatMap()
//			return internalQueryMapping.values().stream().collect(Collectors.toList()).get(0).get(0);
//		}
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

	@Override
	public void initExecutable(Namespaces namespaces) {
		// init all subqueries
		flatSubQueries = subQueries.values().stream().flatMap(List::stream).collect(Collectors.toMap(ManagedQuery::getId, Function.identity()));
		flatSubQueries.values().forEach(mq -> mq.initExecutable(namespaces));
		openSubQueries = new AtomicInteger(flatSubQueries.values().size());
		
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
}
