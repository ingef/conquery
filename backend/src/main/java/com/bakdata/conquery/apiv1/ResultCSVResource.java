package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.apiv1.ResourceConstants.DATASET;
import static com.bakdata.conquery.apiv1.ResourceConstants.QUERY;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.eclipse.jetty.io.EofException;

import com.bakdata.conquery.apiv1.URLBuilder.URLBuilderPath;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryToCSVRenderer;
import com.bakdata.conquery.models.worker.Namespaces;

import io.dropwizard.auth.Auth;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Path("datasets/{" + DATASET + "}/result/")

@Slf4j
public class ResultCSVResource {

	private static final PrintSettings PRINT_SETTINGS = new PrintSettings(true, ConqueryConfig.getInstance().getCsv().getColumnNamerScript());
	public static final URLBuilderPath GET_CSV_PATH = new URLBuilderPath(ResultCSVResource.class, "getAsCSV");
	private final Namespaces namespaces;
	private final ConqueryConfig config;

	@GET
	@Path("{" + QUERY + "}.csv")
	@Produces(AdditionalMediaTypes.CSV)
	public Response getAsCSV(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedExecutionId queryId) {
		authorize(user, datasetId, Ability.READ);
		authorize(user, QueryPermission.INSTANCE.instancePermission(Ability.DOWNLOAD, queryId));
		authorize(user, queryId, Ability.READ);
		
		try {
			ManagedExecution exec = namespaces.getMetaStorage().getExecution(queryId);
			Stream<String> csv = new QueryToCSVRenderer().toCSV(PRINT_SETTINGS, exec.toResultQuery());

			log.info("Querying results for {}", queryId);
			StreamingOutput out = new StreamingOutput() {

				@Override
				public void write(OutputStream os) throws WebApplicationException {
					try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
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
}
