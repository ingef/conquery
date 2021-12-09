package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.JOB_ID;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.FullExecutionStatus;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.config.auth.AuthenticationConfig;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.jobs.JobManagerStatus;
import com.bakdata.conquery.models.messages.network.specific.CancelJobMessage;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.bakdata.conquery.resources.admin.ui.AdminUIResource;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.auth.Auth;

@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Produces(ExtraMimeTypes.JSON_STRING)
@Path("/")
public class AdminResource {

    @Inject
    private AdminProcessor processor;

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

        for (ShardNodeInformation info : processor.getDatasetRegistry().getShardNodes().values()) {
            info.send(new CancelJobMessage(jobId));
        }

        return Response
                .seeOther(UriBuilder.fromPath("/admin/").path(AdminUIResource.class, "getJobs").build())
                .build();
    }

    @GET
    @Path("/jobs/")
    public ImmutableMap<String, JobManagerStatus> getJobs() {
        return processor.getJobs();
    }

    @GET
    @Path("logout")
    public Response logout(@Context ContainerRequestContext requestContext) {
    	// Invalidate all cookies. At the moment the adminEnd uses cookies only for authentication, so this does not interfere with other things
		final NewCookie[] expiredCookies = requestContext.getCookies().keySet().stream().map(AuthenticationConfig::expireCookie).toArray(NewCookie[]::new);
		return Response.ok().cookie(expiredCookies).build();
    }

    @GET
    @Path("/queries")
    public FullExecutionStatus[] getQueries(@Auth Subject currentUser, @QueryParam("limit") OptionalLong limit, @QueryParam("since") Optional<String> since) {
        final MetaStorage storage = processor.getStorage();
        final DatasetRegistry datasetRegistry = processor.getDatasetRegistry();
        return storage.getAllExecutions().stream()
					.map(t -> {
						try {
							return t.buildStatusFull(storage, currentUser, datasetRegistry, processor.getConfig());
						} catch (ConqueryError e) {
							// Initialization of execution probably failed, so we construct a status based on the overview status
							final FullExecutionStatus fullExecutionStatus = new FullExecutionStatus();
							t.setStatusBase(currentUser, fullExecutionStatus);
							fullExecutionStatus.setStatus(ExecutionState.FAILED);
							fullExecutionStatus.setError(e);
							return fullExecutionStatus;
						}
					})
					.filter(t -> t.getCreatedAt().toLocalDate().isEqual(since.map(LocalDate::parse).orElse(LocalDate.now())))
					.limit(limit.orElse(100))
					.toArray(FullExecutionStatus[]::new);
    }
}