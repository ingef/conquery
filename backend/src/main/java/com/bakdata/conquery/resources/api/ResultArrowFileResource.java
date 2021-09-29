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
import com.bakdata.conquery.io.result.arrow.ResultArrowFileProcessor;
import com.bakdata.conquery.models.auth.entities.UserLike;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.ResourceConstants;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("datasets/{" + DATASET + "}/result/")
public class ResultArrowFileResource {
	private static final String GET_RESULT_PATH_METHOD = "get";
	@Inject
	private ResultArrowFileProcessor processor;
	
	@GET
	@Path("{" + QUERY + "}." + FILE_EXTENTION_ARROW_FILE)
	@Produces(AdditionalMediaTypes.ARROW_FILE)
	public Response get(
		@Auth UserLike user,
		@PathParam(DATASET) Dataset dataset,
		@PathParam(QUERY) ManagedExecution<?> query,
		@QueryParam("pretty") Optional<Boolean> pretty) {

		checkSingleTableResult(query);
		log.info("Result for {} download on dataset {} by user {} ({}).", query.getId(), dataset.getId(), user.getId(), user.getName());
		return processor.getArrowFileResult(user, (ManagedExecution<?> & SingleTableResult) query, dataset, pretty.orElse(false));
	}

	public static <E extends ManagedExecution<?> & SingleTableResult> URL getDownloadURL(UriBuilder uriBuilder, E exec) throws MalformedURLException {
		return uriBuilder
				.path(ResultArrowFileResource.class)
				.resolveTemplate(ResourceConstants.DATASET, exec.getDataset().getName())
				.path(ResultArrowFileResource.class, GET_RESULT_PATH_METHOD)
				.resolveTemplate(ResourceConstants.QUERY, exec.getId().toString())
				.build()
				.toURL();
	}
}
