package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.io.result.csv.ResultCsvProcessor;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("datasets/{" + DATASET + "}/result/")
public class ResultCsvResource {

	public static final String GET_CSV_PATH_METHOD = "getAsCsv";
	@Inject
	private ResultCsvProcessor processor;
	@Inject
	private ConqueryConfig config;

	@GET
	@Path("{" + QUERY + "}.csv")
	@Produces(AdditionalMediaTypes.CSV)
	public Response getAsCsv(
		@Auth User user,
		@PathParam(DATASET) DatasetId datasetId,
		@PathParam(QUERY) ManagedExecutionId queryId,
		@HeaderParam("user-agent") String userAgent,
		@QueryParam("charset") String queryCharset,
		@QueryParam("pretty") Optional<Boolean> pretty) 
	{
		log.info("Result for {} download on dataset {} by user {} ({}).", queryId, datasetId, user.getId(), user.getName());
		return processor.getResult(user, datasetId, queryId, userAgent, queryCharset, pretty.orElse(Boolean.TRUE), "csv").build();
	}
}
