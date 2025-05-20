package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.io.result.ResultUtil.checkSingleTableResult;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.OptionalLong;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.io.result.excel.ResultExcelProcessor;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.ResourceConstants;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("result/xlsx")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ResultExcelResource {

	public static final String GET_RESULT_PATH_METHOD = "get";

	private final ResultExcelProcessor processor;

	@GET
	@Path("{" + QUERY + "}.xlsx")
	@Produces(AdditionalMediaTypes.EXCEL)
	public Response get(
			@Auth Subject subject,
			@PathParam(QUERY) ManagedExecutionId execution,
			@HeaderParam(HttpHeaders.USER_AGENT) String userAgent,
			@QueryParam("pretty") @DefaultValue("true") boolean pretty,
			@QueryParam("limit") OptionalLong limit) {
		checkSingleTableResult(execution.resolve());
		log.info("Result for {} download on dataset {} by subject {} ({}).", execution, execution.getDataset(), subject.getId(), subject.getName());
		return processor.createResult(subject, execution, pretty, limit);
	}

	public static <E extends ManagedExecution & SingleTableResult> URL getDownloadURL(UriBuilder uriBuilder, E exec) throws MalformedURLException {
		return uriBuilder
				.path(ResultExcelResource.class)
				.resolveTemplate(ResourceConstants.DATASET, exec.getDataset().getName())
				.path(ResultExcelResource.class, GET_RESULT_PATH_METHOD)
				.resolveTemplate(ResourceConstants.QUERY, exec.getId().toString())
				.build()
				.toURL();
	}
}
