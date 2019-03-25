package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.MANDATOR_NAME;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jersey.AuthCookie;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.JobStatus;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryStatus;
import com.bakdata.conquery.models.query.QueryToCSVRenderer;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.models.worker.SlaveInformation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import io.dropwizard.auth.Auth;
import io.dropwizard.views.View;
import lombok.extern.slf4j.Slf4j;

@Produces(MediaType.TEXT_HTML)
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@PermitAll
@Slf4j
@Path("/")
@AuthCookie
public class AdminUIResource {

	private final ConqueryConfig config;
	private final Namespaces namespaces;
	private final JobManager jobManager;
	private final ObjectMapper mapper;
	private final UIContext context;
	private final AdminUIProcessor processor;

	public AdminUIResource(ConqueryConfig config, Namespaces namespaces, JobManager jobManager, AdminUIProcessor processor) {
		this.config = config;
		this.namespaces = namespaces;
		this.jobManager = jobManager;
		this.mapper = namespaces.injectInto(Jackson.MAPPER);
		this.context = new UIContext(namespaces);
		this.processor = processor;
	}

	@GET
	public View getIndex() {
		return new UIView<>("index.html.ftl", context);
	}

	@GET
	@Path("query")
	public View getQuery() {
		return new UIView<>("query.html.ftl", context);
	}

	@GET
	@Path("/mandators")
	public View getMandators() {
		return new UIView<>("mandators.html.ftl", context, processor.getAllMandators());
	}

	@POST
	@Path("/mandators")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response postMandator(
		@NotEmpty @FormDataParam("mandantor_name") String name,
		@NotEmpty @FormDataParam("mandantor_id") String idString) throws JSONException {
		processor.createMandator(name, idString);
		return Response.ok().build();
	}
	
	/**
	 * End point for retrieving information about a specific mandator.
	 * @param mandatorId Unique id of the mandator.
	 * @return A view holding the information about the mandator.
	 */
	@GET @Path("/mandators/{"+ MANDATOR_NAME +"}")
	public View getMandator(@PathParam(MANDATOR_NAME)MandatorId mandatorId) {
		return new UIView<>("mandator.html.ftl", context, processor.getMandatorContent(mandatorId));
	}
	
	@POST
	@Path("/permissions/")
	@Consumes(ExtraMimeTypes.JSON_STRING)
	public Response createPermission(
		ConqueryPermission permission) throws JSONException {
		processor.createPermission(permission);
		return Response.ok().build();
	}
	
	@DELETE
	@Path("/permissions/")
	@Consumes(ExtraMimeTypes.JSON_STRING)
	public Response deletePermission(
		ConqueryPermission permission) throws JSONException {
		processor.deletePermission(permission);
		return Response.ok().build();
	}

	@Produces(ExtraMimeTypes.CSV_STRING)
	@Consumes(ExtraMimeTypes.JSON_STRING)
	@POST
	@Path("/query")
	public String query(@Auth User user, IQuery query) throws JSONException {
		ManagedQuery managed = namespaces.getNamespaces().iterator().next().getQueryManager().createQuery(query, user);

		managed.awaitDone(1, TimeUnit.DAYS);

		if (managed.getStatus() == QueryStatus.FAILED) {
			throw new IllegalStateException("Query failed");
		}

		return new QueryToCSVRenderer(namespaces.getNamespaces().iterator().next())
			.toCSV(managed)
			.collect(Collectors.joining("\n"));
	}

	@GET
	@Path("/jobs/")
	public View getJobs() {
		Map<String, List<JobStatus>> status = ImmutableMap
			.<String, List<JobStatus>>builder()
			.put("Master", jobManager.reportStatus())
			.putAll(
				namespaces
					.getSlaves()
					.values()
					.stream()
					.collect(Collectors.toMap(
						si -> Objects.toString(si.getRemoteAddress()),
						SlaveInformation::getJobManagerStatus
					))
			)
			.build();
		return new UIView<>("jobs.html.ftl", context, status);
	}
}
