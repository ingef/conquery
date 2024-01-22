package com.bakdata.conquery.io.result;

import java.util.function.Function;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.execution.ResultAsset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Interface for executions, whose final result is produced externally.
 */
public interface ExternalResult {


	/**
	 * Implementations should use the {@link com.bakdata.eva.resources.ExternalResultResource#getDownloadURL(UriBuilder, ManagedExecution, String)}
	 * to build the url for the asset.
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
}
