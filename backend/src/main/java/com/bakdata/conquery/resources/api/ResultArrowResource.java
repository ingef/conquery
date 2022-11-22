package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.io.result.ResultUtil.checkSingleTableResult;
import static com.bakdata.conquery.resources.ResourceConstants.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.io.result.arrow.ResultArrowProcessor;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.ResourceConstants;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("result/")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ResultArrowResource {

	private final ResultArrowProcessor processor;

	@GET
	@Path("{" + QUERY + "}." + FILE_EXTENTION_ARROW_FILE)
	@Produces(AdditionalMediaTypes.ARROW_FILE)
	public Response getFile(
			@Auth Subject subject,
			@PathParam(QUERY) ManagedExecution<?> query,
			@HeaderParam(HttpHeaders.USER_AGENT) String userAgent,
			@QueryParam("pretty") Optional<Boolean> pretty) {

		checkSingleTableResult(query);
		log.info("Result for {} download on dataset {} by subject {} ({}).", query.getId(), query.getDataset().getId(), subject.getId(), subject.getName());
		return processor.createResultFile(subject, query, query.getDataset(), pretty.orElse(false));
	}

	public static <E extends ManagedExecution<?> & SingleTableResult> URL getFileDownloadURL(UriBuilder uriBuilder, E exec) throws MalformedURLException {
		return uriBuilder
				.path(ResultArrowResource.class)
				.resolveTemplate(ResourceConstants.DATASET, exec.getDataset().getName())
				.path(ResultArrowResource.class, "getFile")
				.resolveTemplate(ResourceConstants.QUERY, exec.getId().toString())
				.build()
				.toURL();
	}


	public static <E extends ManagedExecution<?> & SingleTableResult> URL getStreamDownloadURL(UriBuilder uriBuilder, E exec) throws MalformedURLException {
		return uriBuilder
				.path(ResultArrowResource.class)
				.resolveTemplate(ResourceConstants.DATASET, exec.getDataset().getName())
				.path(ResultArrowResource.class, "getStream")
				.resolveTemplate(ResourceConstants.QUERY, exec.getId().toString())
				.build()
				.toURL();
	}

	@GET
	@Path("{" + QUERY + "}." + FILE_EXTENTION_ARROW_STREAM)
	@Produces(AdditionalMediaTypes.ARROW_STREAM)
	public Response getStream(
			@Auth Subject subject,
			@PathParam(DATASET) Dataset dataset,
			@PathParam(QUERY) ManagedExecution<?> execution,
			@HeaderParam(HttpHeaders.USER_AGENT) String userAgent,
			@QueryParam("pretty") Optional<Boolean> pretty) {
		checkSingleTableResult(execution);
		log.info("Result for {} download on dataset {} by subject {} ({}).", execution, dataset, subject.getId(), subject.getName());
		return processor.createResultStream(subject, execution, dataset, pretty.orElse(false));
	}
}
