package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.io.result.csv.QueryToCSVRenderer;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.execution.ResultProcessor;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.mapping.ExternalEntityId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.univocity.parsers.csv.CsvWriter;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.io.EofException;

@Slf4j
@Path("datasets/{" + DATASET + "}/result/")
public class ResultCsvResource {

	public static final String GET_CSV_PATH_METHOD = "getAsCsv";
	@Inject
	private ResultProcessor processor;

	@GET
	@Path("{" + QUERY + "}.csv")
	@Produces(AdditionalMediaTypes.CSV)
	public Response getAsCsv(
		@Auth User user,
		@PathParam(DATASET) Dataset dataset,
		@PathParam(QUERY) ManagedExecution query,
		@HeaderParam("user-agent") String userAgent,
		@QueryParam("charset") String queryCharset,
		@QueryParam("pretty") Optional<Boolean> pretty) 
	{
		log.info("Result for {} download on dataset {} by user {} ({}).", query, dataset, user.getId(), user.getName());
		return processor.getResult(user, dataset, query, userAgent, queryCharset, pretty.orElse(Boolean.TRUE), "csv").build();
	}

	public static StreamingOutput resultAsStreamingOutput(ManagedExecutionId id, PrintSettings settings, List<ManagedQuery> queries, Function<EntityResult,ExternalEntityId> idMapper, Charset charset, String lineSeparator, CsvWriter writer, List<String> header) {
		Stream<String> csv = QueryToCSVRenderer.toCSV(settings, queries, idMapper, writer, header);

		StreamingOutput out = new StreamingOutput() {

			@Override
			public void write(OutputStream os) throws WebApplicationException {
				try (BufferedWriter writer = new BufferedWriter(
					new OutputStreamWriter(
						os,
						charset))) {
					Iterator<String> it = csv.iterator();
					while (it.hasNext()) {
						writer.write(it.next());
						writer.write(lineSeparator);
					}
					writer.flush();
				}
				catch (EofException e) {
					log.info("User canceled download of {}", id);
				}
				catch (Exception e) {
					throw new WebApplicationException("Failed to load result " + id, e);
				}
			}
		};
		return out;
	}
}
