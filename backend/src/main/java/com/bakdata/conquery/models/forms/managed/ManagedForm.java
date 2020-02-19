package com.bakdata.conquery.models.forms.managed;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.URLBuilder;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdCollector;
import com.bakdata.eva.forms.common.Form;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Getter
@Setter
@ToString
@Slf4j
@CPSType(base = ManagedExecution.class, id = "MANAGED_FORM")
public class ManagedForm extends ManagedExecution {

	protected Form form;

	@JsonIgnore
	private transient ListenableFuture<ExecutionState> formFuture;

	/**
	 * Represents the mapping of result matrices with their output name to their actual queries.
	 * The output name is later used to generate the name of the CSV file.
	 */
	protected Map<String,List<ManagedQuery>> internalQueryMapping;

	public ManagedForm(Form form, Namespace namespace, UserId owner) {
		super(namespace, owner);
		this.form = form;
	}

	public void executeForm(ListeningExecutorService pool, MasterMetaStorage storage) {
		setState(ExecutionState.RUNNING);
		formFuture = pool.submit(() -> execute(storage));
		Futures.addCallback(
			formFuture,
			new FutureCallback<ExecutionState>() {
				@Override
				public void onSuccess(ExecutionState result) {
					if(result == ExecutionState.DONE) {
						finish();
					}
					else {
						fail();
					}
				}
				@Override
				public void onFailure(Throwable t) {
					log.error("Failed executing form "+form.getId(), t);
					fail();
				}
			},
			MoreExecutors.directExecutor()
		);
	}
	
	protected ExecutionState execute(MasterMetaStorage storage) throws JSONException, IOException {
		internalQueryMapping = form.executeQuery(
			getNamespace().getStorage().getDataset(),
			getNamespace().getStorage().getMetaStorage().getUser(getOwner()),
			getNamespace().getNamespaces()
		);
		for(List<ManagedQuery> internalQueries : internalQueryMapping.values()) {
			for(ManagedQuery internalQuery : internalQueries) {
				internalQuery.awaitDone(1, TimeUnit.DAYS);
				if(internalQuery.getState() != ExecutionState.DONE) {
					return ExecutionState.FAILED;
				}
			}
		}
		return ExecutionState.DONE;
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
		if(internalQueryMapping.size() == 1 && internalQueryMapping.values().stream().collect(Collectors.toList()).get(0).size() == 1) {
			// Get the query, only if there is only one in the whole execution
			return internalQueryMapping.values().stream().collect(Collectors.toList()).get(0).get(0);
		}
		throw new UnsupportedOperationException("Can't return the result query of a multi query form");
	}

	@Override
	public Set<NamespacedId> getUsedNamespacedIds() {
		NamespacedIdCollector collector = new NamespacedIdCollector();
		form.visit(collector);
		return collector.getIds();
	}
}
