package com.bakdata.conquery.resources.api;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.io.result.excel.ResultExcelProcessor;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.util.ResourceUtil;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import static com.bakdata.conquery.io.result.ResultUtil.checkSingleTableResult;
import static com.bakdata.conquery.resources.ResourceConstants.*;

@Slf4j
@Path("datasets/{" + DATASET + "}/result/")
public class ResultExcelResource {

	public static final String GET_RESULT_PATH_METHOD = "get";

	@Inject
	private ResultExcelProcessor processor;
	
	@GET
	@Path("{" + QUERY + "}.xlsx")
	@Produces(AdditionalMediaTypes.ARROW_FILE)
	public Response get(
		@Auth User user,
		@PathParam(DATASET) DatasetId datasetId,
		@PathParam(QUERY) ManagedExecution<?> execution,
		@QueryParam("pretty") Optional<Boolean> pretty) {
		checkSingleTableResult(execution);
		log.info("Result for {} download on dataset {} by user {} ({}).", execution.getId(), datasetId, user.getId(), user.getName());
		return processor.getExcelResult(user, (ManagedExecution<?> & SingleTableResult) execution, datasetId, pretty.orElse(true));
	}

	public static <E extends ManagedExecution<?> & SingleTableResult> URL getDownloadURL(UriBuilder uriBuilder, E exec) throws MalformedURLException {
		return uriBuilder
				.path(ResultExcelResource.class)
				.resolveTemplate(ResourceConstants.DATASET, exec.getDataset().getName())
				.path(ResultExcelResource.class, GET_RESULT_PATH_METHOD)
				.resolveTemplate(ResourceConstants.QUERY, exec.getId().toString())
				.build()
				.toURL();
	}
}
