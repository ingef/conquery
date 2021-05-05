package com.bakdata.conquery.resources.api;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.io.result.excel.ResultExcelProcessor;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static com.bakdata.conquery.resources.ResourceConstants.*;

@Slf4j
@Path("datasets/{" + DATASET + "}/result/")
public class ResultExcelResource {
	@Inject
	private ResultExcelProcessor processor;
	
	@GET
	@Path("{" + QUERY + "}.xlsx")
	@Produces(AdditionalMediaTypes.ARROW_FILE)
	public Response get(
		@Auth User user,
		@PathParam(DATASET) DatasetId datasetId,
		@PathParam(QUERY) ManagedExecutionId executionId,
		@QueryParam("pretty") Optional<Boolean> pretty) {
		log.info("Result for {} download on dataset {} by user {} ({}).", executionId, datasetId, user.getId(), user.getName());
		return processor.getExcelResult(user, executionId, datasetId, pretty.orElse(false));
	}
}
