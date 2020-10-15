package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.models.execution.ResultProcessor.getResult;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingState;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryToCSVRenderer;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import io.dropwizard.auth.Auth;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.io.EofException;

@AllArgsConstructor
@Path("datasets/{" + DATASET + "}/result/")

@Slf4j
public class ResultCSVResource {

	public static final String GET_CSV_PATH_METHOD = "getAsCsv";
	private final DatasetRegistry namespaces;
	private final ConqueryConfig config;

	@GET
	@Path("{" + QUERY + "}.csv")
	@Produces(AdditionalMediaTypes.CSV)
	public Response getAsCsv(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedExecutionId queryId, @QueryParam("charset") String queryCharset, @HeaderParam("user-agent") String userAgent, @HeaderParam("pretty") Optional<Boolean> pretty) {
		log.info("Result for {} download on dataset {} by user {} ({}).", queryId, datasetId, user.getId(), user.getName());
		return getResult(user, datasetId, queryId, userAgent, queryCharset, pretty.orElse(Boolean.TRUE), namespaces, config).build();
	}

	public static StreamingOutput resultAsStreamingOutput(ManagedExecutionId id, PrintSettings settings, List<ManagedQuery> queries, IdMappingState state, Charset charset, String lineSeparator) {
		Stream<String> csv = QueryToCSVRenderer.toCSV(settings, queries, state);

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
