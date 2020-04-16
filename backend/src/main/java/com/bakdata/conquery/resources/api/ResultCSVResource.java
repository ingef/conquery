package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorizeDownloadDatasets;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.apiv1.URLBuilder.URLBuilderPath;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingState;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryToCSVRenderer;
import com.bakdata.conquery.models.worker.Namespaces;
import io.dropwizard.auth.Auth;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.io.EofException;

@AllArgsConstructor
@Path("datasets/{" + DATASET + "}/result/")

@Slf4j
public class ResultCSVResource {

	public static final URLBuilderPath GET_CSV_PATH = new URLBuilderPath(
		ResultCSVResource.class, "getAsCsv");
	private final Namespaces namespaces;
	private final ConqueryConfig config;

	@GET
	@Path("{" + QUERY + "}.csv")
	@Produces(AdditionalMediaTypes.CSV)
	public Response getAsCsv(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedExecutionId queryId, @HeaderParam("user-agent") String userAgent) {
		authorize(user, datasetId, Ability.READ);
		authorize(user, queryId, Ability.READ);

		ManagedExecution<?> exec = namespaces.getMetaStorage().getExecution(queryId);
		
		// Check if user is permitted to download on all datasets that were referenced by the query
		authorizeDownloadDatasets(user, exec);

		IdMappingState mappingState = config.getIdMapping().initToExternal(user, exec);

		// Get the locale extracted by the LocaleFilter
		PrintSettings settings = new PrintSettings(true, I18n.LOCALE.get());

		try {
			Stream<String> csv = QueryToCSVRenderer.toCSV(settings, exec.toResultQuery(), mappingState);
			final Charset charset = determineCharset(userAgent);

			log.info("Querying results for {}", queryId);
			StreamingOutput out = new StreamingOutput() {
				@Override
				public void write(OutputStream os) throws WebApplicationException {
					try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
							os,
							charset
						))) {
						Iterator<String> it = csv.iterator();
						while (it.hasNext()) {
							writer.write(it.next());
							writer.write(config.getCsv().getLineSeparator());
						}
						writer.flush();
					}
					catch (EofException e) {
						log.info("User canceled download of {}", queryId);
					}
					catch (Exception e) {
						throw new WebApplicationException("Failed to load result " + queryId, e);
					}
				}
			};

			return Response.ok(out).build();
		}
		catch (NoSuchElementException e) {
			throw new WebApplicationException(e, Status.NOT_FOUND);
		}
	}

	private Charset determineCharset(String userAgent) {
		return userAgent.toLowerCase().contains("windows") ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_8;
	}
}
