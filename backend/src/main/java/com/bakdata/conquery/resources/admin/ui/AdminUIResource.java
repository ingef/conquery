package com.bakdata.conquery.resources.admin.ui;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.config.ConqueryConfig;
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
	
	public AdminUIResource(ConqueryConfig config, Namespaces namespaces, JobManager jobManager) {
		this.config = config;
		this.namespaces = namespaces;
		this.jobManager = jobManager;
		this.mapper = namespaces.injectInto(Jackson.MAPPER);
		this.context = new UIContext(namespaces);
	}

	@GET
	public View getIndex() {
		return new UIView<>("index.html.ftl", context);
	}
	
	@GET @Path("query")
	public View getQuery() {
		return new UIView<>("query.html.ftl", context);
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