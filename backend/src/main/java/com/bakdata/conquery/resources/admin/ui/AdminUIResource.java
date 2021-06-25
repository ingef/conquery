package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.JOB_ID;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.models.jobs.JobManagerStatus;
import com.bakdata.conquery.models.messages.network.specific.CancelJobMessage;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HAdmin;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import groovy.lang.GroovyShell;
import io.dropwizard.auth.Auth;
import io.dropwizard.views.View;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

@Produces(MediaType.TEXT_HTML)
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Path("/")
@RequiredArgsConstructor(onConstructor_=@Inject)
public class AdminUIResource extends HAdmin {
	
	public static final String[] AUTO_IMPORTS = Stream
		.of(
			LocalDate.class,
			Range.class,
			DatasetId.class
		)
		.map(Class::getName)
		.toArray(String[]::new);

	private final AdminProcessor processor;
	private final UIProcessor uiProcessor;

	@GET
	public View getIndex() {
		return new UIView<>("index.html.ftl", uiProcessor.getUIContext());
	}

	@GET
	@Path("script")
	public View getScript() {
		return new UIView<>("script.html.ftl", uiProcessor.getUIContext());
	}

	/**
	 * Execute script and serialize value with {@link Objects#toString}.
	 * Used in admin UI for minor scripting.
	 */
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.TEXT_PLAIN)
	@POST
	@Path("/script")
	public String executeScript(@Auth User user, String script) throws JSONException {
		return Objects.toString(executeScript(script));
	}

	/**
	 * Execute script and serialize return value as Json.
	 * Useful for configuration and verification scripts.
	 */
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.TEXT_PLAIN)
	@POST
	@Path("/script")
	public String executeScriptJson(@Auth User user, String script) throws JSONException, JsonProcessingException {
		return Jackson.MAPPER.writeValueAsString(executeScript(script));
	}

	private Object executeScript(String script) {
		CompilerConfiguration config = new CompilerConfiguration();
		config.addCompilationCustomizers(new ImportCustomizer().addImports(AUTO_IMPORTS));
		GroovyShell groovy = new GroovyShell(config);
		groovy.setProperty("datasetRegistry", processor.getDatasetRegistry());
		groovy.setProperty("jobManager", processor.getJobManager());
		groovy.setProperty("config", processor.getConfig());
		groovy.setProperty("storage", processor.getStorage());

		try {
			return groovy.evaluate(script);
		}
		catch(Exception e) {
			return ExceptionUtils.getStackTrace(e);
		}
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Path("/jobs/{" + JOB_ID + "}/cancel")
	public Response cancelJob(@PathParam(JOB_ID)UUID jobId) {

		processor.getJobManager().cancelJob(jobId);

		for (ShardNodeInformation info : processor.getDatasetRegistry().getShardNodes().values()) {
			info.send(new CancelJobMessage(jobId));
		}

		return Response
			.seeOther(UriBuilder.fromPath("/admin/").path(AdminUIResource.class, "getJobs").build())
			.build();
	}

	@GET
	@Path("/jobs/")
	public View getJobs() {
		Map<String, JobManagerStatus> status =
				ImmutableMap.<String, JobManagerStatus>builder()
						.put("ManagerNode", processor.getJobManager().reportStatus())
						// Namespace JobManagers on ManagerNode
						.putAll(
								processor.getDatasetRegistry().getDatasets().stream()
										 .collect(Collectors.toMap(
												 ns -> String.format("ManagerNode::%s", ns.getDataset().getId()),
												 ns -> ns.getJobManager().reportStatus()
										 )))
						// Remote Worker JobManagers
						.putAll(
								processor
										.getDatasetRegistry()
										.getShardNodes()
										.values()
										.stream()
										.collect(Collectors.toMap(
												si -> Objects.toString(si.getRemoteAddress()),
												ShardNodeInformation::getJobManagerStatus
										))
						)
						.build();
		return new UIView<>("jobs.html.ftl", uiProcessor.getUIContext(), status);
	}

	@POST @Path("/jobs") @Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response addDemoJob() {
		processor.getJobManager().addSlowJob(new Job() {
			private final UUID id = UUID.randomUUID();
			@Override
			public void execute() {
				getProgressReporter().setMax(100);

				while(!getProgressReporter().isDone() && !isCancelled()) {
					getProgressReporter().report(1);

					if(getProgressReporter().getProgress() >= 100) {
						getProgressReporter().done();
					}

					Uninterruptibles.sleepUninterruptibly((int)(Math.random()*200), TimeUnit.MILLISECONDS);
				}
			}

			@Override
			public String getLabel() {
				return "Demo "+id;
			}
		});
		
		return Response
			.seeOther(UriBuilder.fromPath("/admin/").path(AdminUIResource.class, "getJobs").build())
			.build();
	}
}
