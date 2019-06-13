package com.bakdata.eva.forms.managed;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.apiv1.URLBuilder;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespace;
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

	protected ManagedQuery internalQuery;

	public ManagedForm(Form form, Namespace namespace, UserId owner) {
		super(namespace, owner);
		this.form = form;
	}

	public void executeForm(ListeningExecutorService pool) {
		setState(ExecutionState.RUNNING);
		formFuture = pool.submit(this::execute);
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
	
	protected ExecutionState execute() throws JSONException, IOException {
		internalQuery = form.executeQuery(
			getNamespace().getStorage().getDataset(),
			getNamespace().getStorage().getMetaStorage().getUser(getOwner()),
			getNamespace().getNamespaces()
		);
		internalQuery.awaitDone(1, TimeUnit.DAYS);
		if(internalQuery.getState() == ExecutionState.DONE) {
			return ExecutionState.DONE;
		}
		else {
			return ExecutionState.FAILED;
		}
	}
	
	public ExecutionStatus buildStatus(URLBuilder url) {
		ExecutionStatus status = super.buildStatus(url);
		if(internalQuery != null) {
			Long numberOfResults = Long.valueOf(internalQuery.fetchContainedEntityResult().count());
			status.setNumberOfResults(numberOfResults);
		}
		return status;
	}

	@Override
	public ManagedQuery toResultQuery() {
		return internalQuery;
	}
}
