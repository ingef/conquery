package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.io.result.ResultUtil.checkSingleTableResult;
import static com.bakdata.conquery.resources.ResourceConstants.FILE_EXTENTION_PARQUET;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.OptionalLong;

import com.bakdata.conquery.io.result.parquet.ResultParquetProcessor;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.ResourceConstants;
import io.dropwizard.auth.Auth;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("result/parquet")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ResultParquetResource {

	public static final String PARQUET_MEDIA_TYPE_STRING = "application/vnd.apache.parquet";

	private final ResultParquetProcessor processor;

	public static <E extends ManagedExecution & SingleTableResult> URL getDownloadURL(UriBuilder uriBuilder, E exec) throws MalformedURLException {
		return uriBuilder
				.path(ResultParquetResource.class)
				.resolveTemplate(ResourceConstants.DATASET, exec.getDataset().getName())
				.path(ResultParquetResource.class, "getFile")
				.resolveTemplate(ResourceConstants.QUERY, exec.getId().toString())
				.build()
				.toURL();
	}

	@GET
	@Path("{" + QUERY + "}." + FILE_EXTENTION_PARQUET)
	@Produces(PARQUET_MEDIA_TYPE_STRING)
	public Response getFile(
			@Auth Subject subject,
			@PathParam(QUERY) ManagedExecution execution,
			@HeaderParam(HttpHeaders.USER_AGENT) String userAgent,
			@QueryParam("pretty") @DefaultValue("false") boolean pretty,
			@QueryParam("limit") OptionalLong limit) {

		checkSingleTableResult(execution);
		log.info("Result for {} download on dataset {} by subject {} ({}).", execution.getId(), execution.getDataset(), subject.getId(), subject.getName());
		return processor.createResultFile(subject, execution, pretty, limit);
	}

}
