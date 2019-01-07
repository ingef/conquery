package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.apiv1.ResourceConstants.DATASET;
import static com.bakdata.conquery.apiv1.ResourceConstants.QUERY;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.apiv1.URLBuilder.URLBuilderPath;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.ResourceUtil;

import io.dropwizard.auth.Auth;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.io.EofException;

@AllArgsConstructor
@Path("datasets/{" + DATASET + "}/result/")
@PermitAll
@Slf4j
public class ResultCSVResource {

	public static final URLBuilderPath GET_CSV_PATH = new URLBuilderPath(ResultCSVResource.class, "getAsCSV");
	private final Namespaces namespaces;
	private final ConqueryConfig config;

	@GET
	@Path("{" + QUERY + "}.csv")
	@Produces(AdditionalMediaTypes.CSV)
	public Response getAsCSV(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedQueryId id) {
		ManagedQuery query = new ResourceUtil(namespaces).getManagedQuery(datasetId, id);
		String csv = query.toCSV(config).collect(Collectors.joining("\n"));

		log.info("Querying results for {}", id);
		StreamingOutput out = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
					writer.write(csv);
					writer.flush();
				} catch (EofException e) {
					log.info("User canceled download of {}", id);
				} catch (Exception e) {
					throw new WebApplicationException("Failed to load result " + id, e);
				}
			}
		};

		return Response.ok(out).build();
	}
}
