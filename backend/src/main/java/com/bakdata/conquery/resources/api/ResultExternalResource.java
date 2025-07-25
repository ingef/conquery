package com.bakdata.conquery.resources.api;


import static com.bakdata.conquery.resources.ResourceConstants.FILENAME;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.net.URI;
import java.net.URISyntaxException;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import com.bakdata.conquery.io.result.external.ExternalResultProcessor;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.forms.managed.ExternalExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.resources.ResourceConstants;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

@Path("result/external")
@Slf4j
public class ResultExternalResource {

	public static final String DOWNLOAD_PATH_METHOD = "download";
	@Inject
	private ExternalResultProcessor processor;


	public static URI getDownloadURL(UriBuilder uriBuilder, ExternalExecution exec, String filename)
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
			@PathParam(QUERY) ManagedExecutionId execution,
			@PathParam(FILENAME) String fileName,
			@HeaderParam("user-agent") String userAgent,
			@QueryParam("charset") String queryCharset
	) {
		log.info("Result download for {} on dataset {} by user {} ({}).", execution, execution.getDataset(), subject.getId(), subject.getName());
		return processor.getResult(subject, execution, fileName);
	}
}
