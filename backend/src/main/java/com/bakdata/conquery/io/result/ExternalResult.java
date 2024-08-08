package com.bakdata.conquery.io.result;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.execution.ResultAsset;
import com.bakdata.conquery.io.external.form.ExternalFormBackendApi;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unimi.dsi.fastutil.Pair;

/**
 * Interface for executions, whose final result is produced externally.
 */
public interface ExternalResult extends ExecutionManager.Result {

	/**
	 * Returns the api object for the external form backend.
	 */
	ExternalFormBackendApi getApi();

	/**
	 * Sets the map of results which reference the result assets of an external execution.
	 */
	void setResultsAssetMap(List<Pair<ResultAsset, AssetBuilder>> assetMap);

	/**
	 * Returns assert builders for all results registered by an {@link com.bakdata.conquery.models.forms.managed.ExternalExecution} (see {@link AssetBuilder}).
	 * The provided assetId is the one that is used by {@link ExternalResult#fetchExternalResult(String)} to retrieve the download.
	 */
	@JsonIgnore
	Stream<AssetBuilder> getResultAssets();

	/**
	 * This usually opens a direct download from the external service that provides the result.
	 * The backend works as a proxy if it forwards this {@link Response} back to the initial requester.
	 */
	@JsonIgnore
	Response fetchExternalResult(String assetId);

	/**
	 * Factory for {@link ResultAsset}s provided by an execution.
	 * This is necessary because the assets (their URLs) are request-dependent
	 */
	@FunctionalInterface
	interface AssetBuilder extends Function<UriBuilder, ResultAsset> {
	}

	User getServiceUser();
}
