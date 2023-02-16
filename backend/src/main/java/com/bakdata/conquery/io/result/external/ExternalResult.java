package com.bakdata.conquery.io.result.external;

import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unimi.dsi.fastutil.Pair;

/**
 * Interface for executions, whose final result is produced externally.
 */
public interface ExternalResult {


	/**
	 * @implNote returns a {@link List} so that the ordering is intentional
	 */
	@JsonIgnore
	List<ExternalResultProcessor.ResultFileReference> getResultFileExtensions();

	/**
	 * This usually opens a direct download from the external service that provides the result.
	 */
	@JsonIgnore
	Pair<Response.ResponseBuilder, MediaType> getExternalResult(ExternalResultProcessor.ResultFileReference resultRef);
}