package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.io.result.ResultUtil.checkSingleTableResult;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.io.result.csv.ResultCsvProcessor;
import com.bakdata.conquery.models.auth.entities.UserLike;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.ResourceConstants;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("datasets/{" + DATASET + "}/result/")
public class ResultCsvResource {

	public static final String GET_RESULT_PATH_METHOD = "getAsCsv";
	@Inject
	private ResultCsvProcessor processor;
	@Inject
	private ConqueryConfig config;

	public static <E extends ManagedExecution<?> & SingleTableResult> URL getDownloadURL(UriBuilder uriBuilder, E exec) throws MalformedURLException {
		return uriBuilder
				.path(ResultCsvResource.class)
				.resolveTemplate(ResourceConstants.DATASET, exec.getDataset().getName())
				.path(ResultCsvResource.class, GET_RESULT_PATH_METHOD)
				.resolveTemplate(ResourceConstants.QUERY, exec.getId().toString())
				.build()
				.toURL();
	}

	@GET
	@Path("{" + QUERY + "}.csv")
	@Produces(AdditionalMediaTypes.CSV)
	public Response getAsCsv(
			@Auth UserLike user,
			@PathParam(DATASET) Dataset datasetId,
			@PathParam(QUERY) ManagedExecution<?> execution,
			@HeaderParam("user-agent") String userAgent,
			@QueryParam("charset") String queryCharset,
			@QueryParam("pretty") Optional<Boolean> pretty)
	{
		checkSingleTableResult(execution);
		log.info("Result for {} download on dataset {} by user {} ({}).", execution, datasetId, user.getId(), user.getName());
		return processor.getResult(user, datasetId, (ManagedExecution<?> & SingleTableResult) execution, userAgent, queryCharset, pretty.orElse(Boolean.TRUE));
	}
}
