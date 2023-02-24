package com.bakdata.conquery.resources.api;


import static com.bakdata.conquery.resources.ResourceConstants.FILENAME;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.io.result.ExternalResult;
import com.bakdata.conquery.io.result.external.ExternalResultProcessor;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.resources.ResourceConstants;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

@Path("result/external")
@Slf4j
public class ResultExternalResource {

	public static final String DOWNLOAD_PATH_METHOD = "download";
	@Inject
	private ExternalResultProcessor processor;


	public static <E extends ManagedExecution & ExternalResult> URI getDownloadURL(UriBuilder uriBuilder, E exec, String filename)
			throws URISyntaxException {
		return uriBuilder
				.path(ResultExternalResource.class)
				.path(ResultExternalResource.class, DOWNLOAD_PATH_METHOD)
				.resolveTemplate(ResourceConstants.QUERY, exec.getId().toString())
				.resolveTemplate(FILENAME, filename)
				.build();
	}


	/**
	 * Retrieve the result of an execution from an external source such as the JupyEnd.
	 *
	 * @param subject      The subject in whose context this request was made
	 * @param execution    The id of the execution, which result is requested
	 * @param fileName     This is only here, so that a download in case of a browser PDF-preview would not use the query id for the file name (indirectly used)
	 * @param userAgent    The user-agent header to guess the charset for the result if needed and not explicitly (unused)
	 * @param queryCharset An optionally explicitly set charset (unused)
	 * @return A response with an entity containing the requested result
	 */
	@GET
	@Path("{" + QUERY + "}/{" + FILENAME + "}")
	public Response download(
			@Auth Subject subject,
			@PathParam(QUERY) ManagedExecution execution,
			@PathParam(FILENAME) String fileName,
			@HeaderParam("user-agent") String userAgent,
			@QueryParam("charset") String queryCharset
	) {
		log.info("Result download for {} on dataset {} by user {} ({}).", execution, execution.getDataset().getId(), subject.getId(), subject.getName());
		return processor.getResult(subject, execution, fileName);
	}
}
