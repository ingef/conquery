package com.bakdata.conquery.models.forms.managed;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;

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
import com.bakdata.conquery.resources.api.ResultExternalResource;
import com.bakdata.conquery.util.AuthUtil;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.google.common.base.Preconditions;
import com.google.common.collect.MoreCollectors;
import it.unimi.dsi.fastutil.Pair;
import lombok.EqualsAndHashCode;
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
public class ExternalExecution extends ManagedForm<ExternalForm> implements ExternalResult {


	private UUID externalTaskId;

	@JsonIgnore
	@EqualsAndHashCode.Exclude
	private ExternalFormBackendApi api;

	@JsonIgnore
	@EqualsAndHashCode.Exclude
	private FormBackendConfig formBackendConfig;

	@JsonIgnore
	@EqualsAndHashCode.Exclude
	private User serviceUser;


	/**
	 * Pairs of external result assets (internal url) and their internal asset builder.
	 * The internal asset builder generates the asset url with the context of a user request.
	 */
	@JsonIgnore
	private List<Pair<ResultAsset, AssetBuilder>> resultsAssetMap = Collections.emptyList();

	@JsonCreator
	protected ExternalExecution(@JacksonInject(useInput = OptBoolean.FALSE) MetaStorage storage) {
		super(storage);
	}

	public ExternalExecution(ExternalForm form, User user, Dataset dataset, MetaStorage storage) {
		super(form, user, dataset, storage);
	}

	@Override
	protected void doInitExecutable() {
		formBackendConfig = getConfig().getPluginConfigs(FormBackendConfig.class)
									   .filter(c -> c.supportsFormType(getSubmittedForm().getFormType()))
									   .collect(MoreCollectors.onlyElement());

		api = formBackendConfig.createApi();
	}

	@Override
	public void start() {

		synchronized (this) {

			if (externalTaskId != null) {
				syncExternalState();
			}

			if (getState() == ExecutionState.RUNNING) {
				throw new ConqueryError.ExecutionProcessingError();
			}

			super.start();

			// Create service user
			serviceUser = formBackendConfig.createServiceUser(getOwner(), getDataset());

			final ExternalTaskState externalTaskState = api.postForm(getSubmitted(), getOwner(), serviceUser, getDataset());

			externalTaskId = externalTaskState.getId();
		}
	}

	private synchronized void syncExternalState() {
		Preconditions.checkNotNull(externalTaskId, "Cannot check external task, because no Id is present");

		final ExternalTaskState formState = api.getFormState(externalTaskId);

		switch (formState.getStatus()) {

			case RUNNING -> {
				setState(ExecutionState.RUNNING);
				setProgress(formState.getProgress().floatValue());
			}
			case FAILURE -> fail(formState.getError());
			case SUCCESS -> {
				resultsAssetMap = registerResultAssets(formState);
				finish(ExecutionState.DONE);
			}
		}
	}

	private List<Pair<ResultAsset, AssetBuilder>> registerResultAssets(ExternalTaskState response) {
		final List<Pair<ResultAsset, AssetBuilder>> assetMap = new ArrayList<>();
		response.getResults().forEach(asset ->
									  {
										  assetMap.add(Pair.of(asset, createResultAssetBuilder(asset)));
									  });
		return assetMap;
	}

	/**
	 * The {@link ResultAsset} is request-dependent, so we can prepare only builder here which takes an url builder.
	 */
	private AssetBuilder createResultAssetBuilder(ResultAsset asset) {
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
		syncExternalState();

		super.setStatusBase(subject, status);
	}

	@Override
	public Stream<AssetBuilder> getResultAssets() {
		return resultsAssetMap.stream().map(Pair::value);
	}

	@Override
	public Response fetchExternalResult(String assetId) {
		final ResultAsset resultRef = resultsAssetMap.stream()
													 .map(Pair::key).filter(a -> a.getAssetId().equals(assetId))
													 .collect(MoreCollectors.onlyElement());

		return api.getResult(resultRef.url());
	}

	@Override
	protected void finish(ExecutionState executionState) {
		if (getState().equals(executionState)) {
			return;
		}
		super.finish(executionState);
		synchronized (this) {
			AuthUtil.cleanUpUserAndBelongings(serviceUser, getStorage());
			serviceUser = null;
		}
	}
}
