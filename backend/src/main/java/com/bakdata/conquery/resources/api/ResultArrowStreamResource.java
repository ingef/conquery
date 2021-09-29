package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.io.result.ResultUtil.checkSingleTableResult;
import static com.bakdata.conquery.resources.ResourceConstants.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.io.result.arrow.ResultArrowStreamProcessor;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.ResourceConstants;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("datasets/{" + DATASET + "}/result/")
public class ResultArrowStreamResource {

	private static final String GET_RESULT_PATH_METHOD = "get";

	@Inject
	private ResultArrowStreamProcessor processor;

	public static URL getDownloadURL(UriBuilder uriBuilder, ManagedExecution<?> exec) throws MalformedURLException {
		return uriBuilder
				.path(ResultArrowStreamResource.class)
				.resolveTemplate(ResourceConstants.DATASET, exec.getDataset().getName())
				.path(ResultArrowStreamResource.class, GET_RESULT_PATH_METHOD)
				.resolveTemplate(ResourceConstants.QUERY, exec.getId().toString())
				.build()
				.toURL();
	}

	@GET
	@Path("{" + QUERY + "}." + FILE_EXTENTION_ARROW_STREAM)
	@Produces(AdditionalMediaTypes.ARROW_STREAM)
	public Response get(
		@Auth User user,
		@PathParam(DATASET) Dataset dataset,
		@PathParam(QUERY) ManagedExecution<?> execution,
		@QueryParam("pretty") Optional<Boolean> pretty)
	{
		checkSingleTableResult(execution);
		log.info("Result for {} download on dataset {} by user {} ({}).", execution, dataset, user.getId(), user.getName());
		return processor.getArrowStreamResult(user, (ManagedExecution<?> & SingleTableResult)execution, dataset, pretty.orElse(false));
	}
}
