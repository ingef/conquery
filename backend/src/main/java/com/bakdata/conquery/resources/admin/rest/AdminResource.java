package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.INDEX_SERVICE_PATH_ELEMENT;
import static com.bakdata.conquery.resources.ResourceConstants.JOB_ID;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.jobs.JobManagerStatus;
import com.bakdata.conquery.models.messages.network.specific.CancelJobMessage;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.bakdata.conquery.resources.admin.ui.AdminUIResource;
import io.dropwizard.auth.Auth;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import lombok.RequiredArgsConstructor;

@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Produces(ExtraMimeTypes.JSON_STRING)
@Path("/")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AdminResource {

	private final AdminProcessor processor;

	/**
	 * Execute script and serialize value with {@link Objects#toString}.
	 * Used in admin UI for minor scripting.
	 */
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.TEXT_PLAIN)
	@POST
	@Path("/script")
	public String executeScript(@Auth Subject user, String script) {
		return Objects.toString(processor.executeScript(script));
	}

	/**
	 * Execute script and serialize return value as Json.
	 * Useful for configuration and verification scripts.
	 */
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.TEXT_PLAIN)
	@POST
	@Path("/script")
	public Object executeScriptJson(@Auth Subject user, String script) {
		return processor.executeScript(script);
	}


	@POST
	@Path("/jobs/{" + JOB_ID + "}/cancel")
	public Response cancelJob(@PathParam(JOB_ID) UUID jobId) {

		processor.getJobManager().cancelJob(jobId);

		for (ShardNodeInformation info : processor.getNodeProvider().get()) {
			info.send(new CancelJobMessage(jobId));
		}

		return Response
				.seeOther(UriBuilder.fromPath("/admin/").path(AdminUIResource.class, "getJobs").build())
				.build();
	}

	@GET
	@Path("/jobs/")
	public Collection<JobManagerStatus> getJobs() {
		return processor.getJobs();
	}

	@GET
	@Path("/busy")
	public boolean isBusy() {
		return processor.isBusy();
	}

	@GET
	@Path("/queries")
	public FullExecutionStatus[] getQueries(@Auth Subject currentUser, @QueryParam("limit") OptionalLong maybeLimit, @QueryParam("since") Optional<String> maybeSince) {

		final LocalDate since = maybeSince.map(LocalDate::parse).orElse(LocalDate.now());
		final long limit = maybeLimit.orElse(100);

		final MetaStorage storage = processor.getStorage();


		return storage.getAllExecutions()
					  .filter(t -> t.getCreationTime().toLocalDate().isAfter(since) || t.getCreationTime().toLocalDate().isEqual(since))
					  .limit(limit)
					  .map(t -> {
						  try {
							  return t.buildStatusFull(currentUser);
						  }
						  catch (ConqueryError e) {
							  // Initialization of execution probably failed, so we construct a status based on the overview status
							  final FullExecutionStatus fullExecutionStatus = new FullExecutionStatus();
							  t.setStatusBase(currentUser, fullExecutionStatus);
							  fullExecutionStatus.setStatus(ExecutionState.FAILED);
							  fullExecutionStatus.setError(e);
							  return fullExecutionStatus;
						  }
					  })
					  .toArray(FullExecutionStatus[]::new);
	}

	@POST
	@Path("/" + INDEX_SERVICE_PATH_ELEMENT + "/reset")
	public void resetIndexService() {
		processor.resetIndexService();
	}
}
