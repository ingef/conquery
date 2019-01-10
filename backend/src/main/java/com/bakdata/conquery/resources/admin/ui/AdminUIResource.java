package com.bakdata.conquery.resources.admin.ui;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.util.SinglePrincipalCollection;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.JobStatus;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.models.worker.SlaveInformation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import io.dropwizard.views.View;
import lombok.extern.slf4j.Slf4j;

@Produces(MediaType.TEXT_HTML)
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@PermitAll @Slf4j
@Path("/")
public class AdminUIResource {
	
	private final ConqueryConfig config;
	private final Namespaces namespaces;
	private final JobManager jobManager;
	private final ObjectMapper mapper;
	private final UIContext context;
	private final MasterMetaStorage storage;
	
	public AdminUIResource(ConqueryConfig config, Namespaces namespaces, JobManager jobManager, MasterMetaStorage storage) {
		this.config = config;
		this.namespaces = namespaces;
		this.jobManager = jobManager;
		this.mapper = namespaces.injectInto(Jackson.MAPPER);
		this.context = new UIContext(namespaces);
		this.storage = storage;
	}

	@GET
	public View getIndex() {
		return new UIView<>("index.html.ftl", context);
	}
	
	@GET @Path("query")
	public View getQuery() {
		return new UIView<>("query.html.ftl", context);
	}

	@GET @Path("/mandator")
	public View getMandator() {
		return new UIView<>("mandator.html.ftl", context);
	}
	
	@POST @Path("/mandator")  @Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response postMandator(
			@NotEmpty@FormDataParam("mandantor_name")String name,
			@NotEmpty@FormDataParam("mandantor_id")String idString) throws JSONException {
		log.debug("New mandator:\tName: {}\tId: {} ", name, idString);
		MandatorId mandatorId = new MandatorId(idString);
		Mandator mandator = new Mandator(new SinglePrincipalCollection(mandatorId));
		mandator.setLabel(name);
		mandator.setName(name);
		mandator.setStorage(storage);
		storage.addMandator(mandator);
		return Response.ok().build();
	}
	
	@GET @Path("jobs")
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
							si->si.getRemoteAddress().toString(),
							SlaveInformation::getJobManagerStatus
						)
					)
				)
				.build();
		return new UIView<>("jobs.html.ftl", context, status);
	}
}