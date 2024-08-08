package com.bakdata.conquery.models.forms.managed;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.execution.ExecutionStatus;
import com.bakdata.conquery.apiv1.execution.ResultAsset;
import com.bakdata.conquery.apiv1.forms.ExternalForm;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.external.form.ExternalFormBackendApi;
import com.bakdata.conquery.io.external.form.ExternalTaskState;
import com.bakdata.conquery.io.result.ExternalResult;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.FormBackendConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.ExternalResultImpl;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.api.ResultExternalResource;
import com.bakdata.conquery.util.AuthUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import com.google.common.collect.MoreCollectors;
import it.unimi.dsi.fastutil.Pair;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * This execution type is for external form backends which use Conquery as a proxy for their task execution.
 * An {@link ExternalForm} is wrapped in this execution to keep
 * track of it's state using a configured API (see {@link FormBackendConfig} and {@link ExternalFormBackendApi}).
 */
@Slf4j
@CPSType(id = "EXTERNAL_EXECUTION", base = ManagedExecution.class)
@EqualsAndHashCode(callSuper = true, doNotUseGetters = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalExecution extends ManagedForm<ExternalForm> {


	@Getter
	private UUID externalTaskId;

	@JsonIgnore
	@EqualsAndHashCode.Exclude
	private ExecutionManager<?> executionManager;


	public ExternalExecution(ExternalForm form, User user, Dataset dataset, MetaStorage metaStorage) {
		super(form, user, dataset, metaStorage);
	}

	@Override
	protected void doInitExecutable(Namespace namespace) {
		executionManager = namespace.getExecutionManager();
	}



	@Override
	public void start() {

		synchronized (this) {

			if (externalTaskId != null) {
				syncExternalState(executionManager);
			}

			if (getState() == ExecutionState.RUNNING) {
				throw new ConqueryError.ExecutionProcessingError();
			}


			// Create service user
			Dataset dataset = getNamespace().getDataset();
			User originalUser = getOwner();
			FormBackendConfig formBackendConfig = getConfig().getPluginConfigs(FormBackendConfig.class)
															 .filter(c -> c.supportsFormType(getSubmittedForm().getFormType()))
															 .collect(MoreCollectors.onlyElement());
			User serviceUser = formBackendConfig.createServiceUser(originalUser, dataset);
			ExternalFormBackendApi api = formBackendConfig.createApi();

			final ExternalTaskState externalTaskState = api.postForm(getSubmitted(), originalUser, serviceUser, dataset);

			executionManager.addResult(this, new ExternalResultImpl(new CountDownLatch(0), api, serviceUser));

			externalTaskId = externalTaskState.getId();

			super.start();
		}
	}

	private synchronized void syncExternalState(ExecutionManager<?> executionManager) {
		Preconditions.checkNotNull(externalTaskId, "Cannot check external task, because no Id is present");

		final ExternalTaskState formState = this.executionManager.getExternalResult(this.getId()).getApi().getFormState(externalTaskId);

		updateStatus(formState, executionManager);
	}

	private void updateStatus(ExternalTaskState formState, ExecutionManager<?> executionManager) {
		switch (formState.getStatus()) {

			case RUNNING -> {
				setState(ExecutionState.RUNNING);
				setProgress(formState.getProgress().floatValue());
			}
			case FAILURE -> fail(formState.getError(), executionManager);
			case SUCCESS -> {
				List<Pair<ResultAsset, ExternalResult.AssetBuilder>> resultsAssetMap = registerResultAssets(formState);
				this.executionManager.getExternalResult(this.getId()).setResultsAssetMap(resultsAssetMap);
				finish(ExecutionState.DONE, executionManager);
			}
			case CANCELLED -> reset(executionManager);
		}
	}

	private List<Pair<ResultAsset, ExternalResult.AssetBuilder>> registerResultAssets(ExternalTaskState response) {
		final List<Pair<ResultAsset, ExternalResult.AssetBuilder>> assetMap = new ArrayList<>();
		response.getResults().forEach(asset -> assetMap.add(Pair.of(asset, createResultAssetBuilder(asset))));
		return assetMap;
	}

	/**
	 * The {@link ResultAsset} is request-dependent, so we can prepare only builder here which takes an url builder.
	 */
	private ExternalResult.AssetBuilder createResultAssetBuilder(ResultAsset asset) {
		return (uriBuilder) -> {
			try {
				final URI externalDownloadURL = ResultExternalResource.getDownloadURL(uriBuilder.clone(), this, asset.getAssetId());
				return new ResultAsset(asset.label(), externalDownloadURL);
			}
			catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		};
	}

	@Override
	public void setStatusBase(@NonNull Subject subject, @NonNull ExecutionStatus status, Namespace namespace) {
		syncExternalState(namespace.getExecutionManager());

		super.setStatusBase(subject, status, namespace);
	}

	@Override
	public void cancel() {
		//TODO this is no longer called as the ExecutionManager used to call this.
		Preconditions.checkNotNull(externalTaskId, "Cannot check external task, because no Id is present");

		updateStatus(executionManager.getExternalResult(this.getId()).getApi().cancelTask(externalTaskId), executionManager);
	}

	@Override
	public void finish(ExecutionState executionState, ExecutionManager<?> executionManager) {
		if (getState().equals(executionState)) {
			return;
		}
		User serviceUser = executionManager.getExternalResult(this.getId()).getServiceUser();

		super.finish(executionState, executionManager);

		synchronized (this) {
			AuthUtil.cleanUpUserAndBelongings(serviceUser, getMetaStorage());
		}
	}

	@JsonIgnore
	public Stream<ExternalResult.AssetBuilder> getResultAssets() {
		return executionManager.getExternalResult(this.getId()).getResultAssets();
	}
}
