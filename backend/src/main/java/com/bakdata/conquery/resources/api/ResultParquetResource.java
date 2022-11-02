package com.bakdata.conquery.resources.api;


import static com.bakdata.conquery.io.result.ResultUtil.checkSingleTableResult;
import static com.bakdata.conquery.resources.ResourceConstants.*;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
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
import com.bakdata.conquery.io.result.parquet.ResultParquetProcessor;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.ResourceConstants;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("datasets/{" + DATASET + "}/result/")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ResultParquetResource {

	public static final String PARQUET_MEDIA_TYPE_STRING = "application/vnd.apache.parquet";

	private final ResultParquetProcessor processor;

	public static <E extends ManagedExecution<?> & SingleTableResult> URL getDownloadURL(UriBuilder uriBuilder, E exec) throws MalformedURLException {
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
			@PathParam(DATASET) Dataset dataset,
			@PathParam(QUERY) ManagedExecution<?> execution,
			@HeaderParam(HttpHeaders.USER_AGENT) String userAgent,
			@QueryParam("pretty") @DefaultValue("false") boolean pretty) {

		checkSingleTableResult(execution);
		log.info("Result for {} download on dataset {} by subject {} ({}).", execution.getId(), dataset.getId(), subject.getId(), subject.getName());
		return processor.createResultFile(subject, execution, dataset, pretty);
	}

}
