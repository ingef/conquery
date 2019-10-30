package com.bakdata.conquery.resources.admin.ui;

import com.bakdata.conquery.apiv1.FilterSearch;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.models.jobs.JobManagerStatus;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateMatchingStatsMessage;
import com.bakdata.conquery.models.messages.network.specific.CancelJobMessage;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.SlaveInformation;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
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
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.SocketAddress;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bakdata.conquery.resources.ResourceConstants.*;

@Produces(MediaType.TEXT_HTML)
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Path("/")
@RequiredArgsConstructor(onConstructor_=@Inject)
public class AdminUIResource {
	
	public static final String[] AUTO_IMPORTS = Stream
		.of(
			LocalDate.class,
			Range.class,
			DatasetId.class
		)
		.map(Class::getName)
		.toArray(String[]::new);

	private final AdminProcessor processor;

	@GET
	public View getIndex() {
		return new UIView<>("index.html.ftl", processor.getUIContext());
	}

	@GET
	@Path("script")
	public View getScript() {
		return new UIView<>("script.html.ftl", processor.getUIContext());
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
		HttpServletRequest request;
		CompilerConfiguration config = new CompilerConfiguration();
		config.addCompilationCustomizers(new ImportCustomizer().addImports(AUTO_IMPORTS));
		GroovyShell groovy = new GroovyShell(config);
		groovy.setProperty("namespaces", processor.getNamespaces());
		groovy.setProperty("jobManager", processor.getJobManager());

//		{
//			Namespaces namespaces;
//			for (Namespace namespace : namespaces.getNamespaces()) {
//				for (ManagedExecution exec : namespaces.getMetaStorage().getAllExecutions()) {
//					if(exec.getDataset().equals(namespace.getDataset().getId())) {
//						namespace.getQueryManager().executeQuery(((ManagedQuery) exec));
//						Thread.sleep(100);
//					}
//				}
//			}
//		}

		try {
			return groovy.evaluate(script);
		}
		catch(Exception e) {
			return ExceptionUtils.getStackTrace(e);
		}
	}

	@GET
	@Path("/mandators")
	public View getMandators() {
		return new UIView<>("mandators.html.ftl", processor.getUIContext(), processor.getAllMandators());
	}

	@GET
	@Path("datasets")
	public View getDatasets() {
		return new UIView<>("datasets.html.ftl", processor.getUIContext(), processor.getNamespaces().getAllDatasets());
	}

	@POST
	@Path("/mandators")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response postMandator(
		@NotEmpty @FormDataParam("mandator_name") String name,
		@NotEmpty @FormDataParam("mandator_id") String idString) throws JSONException {
		processor.createMandator(name, idString);
		return Response.ok().build();
	}
	
	@DELETE
	@Path("/mandators/{"+ MANDATOR_NAME +"}")
	public Response deleteMandator(@PathParam(MANDATOR_NAME)MandatorId mandatorId) throws JSONException {
		processor.deleteMandator(mandatorId);
		return Response.ok().build();
	}
	
	/**
	 * End point for retrieving information about a specific mandator.
	 * @param mandatorId Unique id of the mandator.
	 * @return A view holding the information about the mandator.
	 */
	@GET @Path("/mandators/{"+ MANDATOR_NAME +"}")
	public View getMandator(@PathParam(MANDATOR_NAME)MandatorId mandatorId) {
		return new UIView<>("mandator.html.ftl", processor.getUIContext(), processor.getMandatorContent(mandatorId));
	}

	@POST @Path("/update-matching-stats/{"+ DATASET_NAME +"}") @Consumes(MediaType.MULTIPART_FORM_DATA)
	public void updateMatchingStats(@Auth User user, @PathParam(DATASET_NAME)DatasetId datasetId) throws JSONException {

		Namespace ns = processor.getNamespaces().get(datasetId);
		ns.sendToAll(new UpdateMatchingStatsMessage());

		FilterSearch.init(processor.getNamespaces(), Collections.singleton(ns.getDataset()));
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Path("/jobs/{" + JOB_ID + "}/cancel")
	public Response cancelJob(@PathParam(JOB_ID)UUID jobId) {

		processor.getJobManager().cancelJob(jobId);

		for (Map.Entry<SocketAddress, SlaveInformation> entry : processor.getNamespaces().getSlaves().entrySet()) {
			SlaveInformation info = entry.getValue();
			info.send(new CancelJobMessage(jobId));
		}

		return Response
			.seeOther(UriBuilder.fromPath("/admin/").path(AdminUIResource.class, "getJobs").build())
			.build();
	}

	@GET
	@Path("/jobs/")
	public View getJobs() {
		Map<String, JobManagerStatus> status = ImmutableMap
			.<String, JobManagerStatus>builder()
			.put("Master", processor.getJobManager().reportStatus())
			.putAll(
				processor
					.getNamespaces()
					.getSlaves()
					.values()
					.stream()
					.collect(Collectors.toMap(
						si -> Objects.toString(si.getRemoteAddress()),
						SlaveInformation::getJobManagerStatus
					))
			)
			.build();
		return new UIView<>("jobs.html.ftl", processor.getUIContext(), status);
	}
	
	@POST @Path("/jobs") @Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response addDemoJob() {
		processor.getJobManager().addSlowJob(new Job() {
			private final UUID id = UUID.randomUUID();
			@Override
			public void execute() {
				while(!progressReporter.isDone() && !isCancelled()) {
					progressReporter.report(0.01d);
					if(progressReporter.getProgress()>=1) {
						progressReporter.done();
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
