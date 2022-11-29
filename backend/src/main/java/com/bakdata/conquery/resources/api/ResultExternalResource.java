package com.bakdata.conquery.resources.api;


import static com.bakdata.conquery.resources.ResourceConstants.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.io.result.external.ExternalResult;
import com.bakdata.conquery.io.result.external.ExternalResultProcessor;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.resources.ResourceConstants;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

/**
 * Endpoint for result request those content is provided externally.
 */
@Path("datasets/{" + DATASET + "}/result/")
@PermitAll
@Slf4j
public class ResultExternalResource {

	public static final String DOWNLOAD_PATH_METHOD = "download";
	public static final String RESULT_FILE_EXTENSION = "download";
	public static final String RESULT_ID = "result-id";
	@Inject
	private ExternalResultProcessor processor;


	public static <E extends ManagedExecution<?> & ExternalResult> URL getDownloadURL(UriBuilder uriBuilder, E exec, ExternalResultProcessor.ResultFileReference resultFileReference)
			throws MalformedURLException {
		return uriBuilder
				.path(ResultExternalResource.class)
				.resolveTemplate(ResourceConstants.DATASET, exec.getDataset().getName())
				.path(ResultExternalResource.class, DOWNLOAD_PATH_METHOD)
				.resolveTemplate(ResourceConstants.QUERY, exec.getId().toString())
				.resolveTemplate(FILENAME, exec.getLabelWithoutAutoLabelSuffix())
				.resolveTemplate(RESULT_FILE_EXTENSION, resultFileReference.fileExtension())
				.queryParam(RESULT_ID, resultFileReference.resultId())
				.build()
				.toURL();
	}


	/**
	 * Retrieve the result of an execution from an external source.
	 *
	 * @param subject       The subject in whose context this request was made
	 * @param datasetId     The dataset in which the execution was run
	 * @param queryId       The id of the execution, which result is requested
	 * @param fileExtension The file extension of the requested result
	 * @param fileName      This is only here, so that a download in case of a browser PDF-preview would not use the query id for the file name (indirectly used)
	 * @param userAgent     The user-agent header to guess the charset for the result if needed and not explicitly (unused)
	 * @param queryCharset  An optionally explicitly set charset (unused)
	 * @param pretty        A flag to indicate that the output should be pretty printed (unused)
	 * @return A response with an entity containing the requested result
	 */
	@GET
	@Path("{" + QUERY + "}/{" + FILENAME + "}.{" + RESULT_FILE_EXTENSION + "}")
	public Response download(
			@Auth Subject subject,
			@PathParam(DATASET) DatasetId datasetId,
			@PathParam(QUERY) ManagedExecutionId queryId,
			@PathParam(RESULT_FILE_EXTENSION) String fileExtension,
			@PathParam(FILENAME) String fileName,
			@HeaderParam("user-agent") String userAgent,
			@QueryParam("charset") String queryCharset,
			@QueryParam("pretty") Optional<Boolean> pretty,
			@QueryParam(RESULT_ID) @NotEmpty String resultId
	) {
		log.info("Result for {} download on dataset {} by user {} ({}).", queryId, datasetId, subject.getId(), subject.getName());
		return processor.getResult(subject, datasetId, queryId, fileName, fileExtension, resultId);
	}
}

