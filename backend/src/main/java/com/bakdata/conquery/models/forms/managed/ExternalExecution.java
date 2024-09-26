package com.bakdata.conquery.models.forms.managed;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.execution.ExecutionStatus;
import com.bakdata.conquery.apiv1.execution.ResultAsset;
import com.bakdata.conquery.apiv1.forms.ExternalForm;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.external.form.ExternalFormBackendApi;
import com.bakdata.conquery.io.external.form.ExternalTaskState;
import com.bakdata.conquery.io.result.ExternalState;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.FormBackendConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.ExternalStateImpl;
import com.bakdata.conquery.models.worker.DatasetRegistry;
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
@Getter
public class ExternalExecution extends ManagedForm<ExternalForm> {


	private UUID externalTaskId;

	public ExternalExecution(ExternalForm form, User user, Dataset dataset, MetaStorage metaStorage, DatasetRegistry<?> datasetRegistry) {
		super(form, user, dataset, metaStorage, datasetRegistry);
	}


	@Override
	protected void doInitExecutable() {
		// Nothing to initialize
	}

	@Override
	public void start() {

		synchronized (this) {

			if (externalTaskId != null) {
				syncExternalState(getExecutionManager());
			}

			// Check after possible sync
			final boolean isRunning = getExecutionManager().tryGetResult(getId())
														   .map(ExecutionManager.State::getState)
														   .map(ExecutionState.RUNNING::equals).orElse(false);
			if (isRunning) {
				throw new ConqueryError.ExecutionProcessingError();
			}


			// Create service user
			final Dataset dataset = getNamespace().getDataset();
			final User originalUser = getOwner();
			final FormBackendConfig formBackendConfig = getConfig().getPluginConfigs(FormBackendConfig.class)
																   .filter(c -> c.supportsFormType(getSubmittedForm().getFormType()))
																   .collect(MoreCollectors.onlyElement());
			final User serviceUser = formBackendConfig.createServiceUser(originalUser, dataset);
			final ExternalFormBackendApi api = formBackendConfig.createApi();

			super.start();

			getExecutionManager().addState(getId(), new ExternalStateImpl(ExecutionState.RUNNING, new CountDownLatch(0), api, serviceUser));

			final ExternalTaskState externalTaskState = api.postForm(getSubmitted(), originalUser, serviceUser, dataset);
			externalTaskId = externalTaskState.getId();
		}
	}

	private synchronized void syncExternalState(ExecutionManager executionManager) {
		Preconditions.checkNotNull(externalTaskId, "Cannot check external task, because no Id is present");

		final Optional<ExternalState> state = executionManager.tryGetResult(getId());
		if (state.isPresent()) {
			final ExternalTaskState formState = state.get().getApi().getFormState(externalTaskId);
			updateStatus(formState);
		}
	}

	private void updateStatus(ExternalTaskState formState) {
		switch (formState.getStatus()) {

			case RUNNING -> setProgress(formState.getProgress().floatValue());
			case FAILURE -> fail(formState.getError());
			case SUCCESS -> {
				final List<Pair<ResultAsset, ExternalState.AssetBuilder>> resultsAssetMap = registerResultAssets(formState);
				final ExternalState state = getExecutionManager().getResult(getId());
				state.setResultsAssetMap(resultsAssetMap);
				finish(ExecutionState.DONE);
			}
			case CANCELLED -> reset();
		}
	}

	private List<Pair<ResultAsset, ExternalState.AssetBuilder>> registerResultAssets(ExternalTaskState response) {
		final List<Pair<ResultAsset, ExternalState.AssetBuilder>> assetMap = new ArrayList<>();
		response.getResults().forEach(asset -> assetMap.add(Pair.of(asset, createResultAssetBuilder(asset))));
		return assetMap;
	}

	@Override
	public synchronized void finish(ExecutionState executionState) {
		if (getState().equals(executionState)) {
			return;
		}

		final ExternalState state = getExecutionManager().getResult(getId());
		final User serviceUser = state.getServiceUser();

		super.finish(executionState);

		AuthUtil.cleanUpUserAndBelongings(serviceUser, getMetaStorage());

	}

	/**
	 * The {@link ResultAsset} is request-dependent, so we can prepare only builder here which takes an url builder.
	 */
	private ExternalState.AssetBuilder createResultAssetBuilder(ResultAsset asset) {
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
	public void setStatusBase(@NonNull Subject subject, @NonNull ExecutionStatus status) {
		if (externalTaskId != null) {
			syncExternalState(getExecutionManager());
		}

		super.setStatusBase(subject, status);
	}

	@Override
	public void cancel() {
		//TODO this is no longer called as the ExecutionManager used to call this.
		Preconditions.checkNotNull(externalTaskId, "Cannot check external task, because no Id is present");

		final ExternalState state = getExecutionManager().getResult(getId());
		updateStatus(state.getApi().cancelTask(externalTaskId));
	}

	@JsonIgnore
	public Stream<ExternalState.AssetBuilder> getResultAssets() {
		final ExternalState state = getExecutionManager().getResult(getId());
		return state.getResultAssets();
	}
}
