package com.bakdata.conquery.resources.admin.rest;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.auth.AuthenticationConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.models.jobs.JobManagerStatus;
import com.bakdata.conquery.models.messages.network.specific.CancelJobMessage;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.bakdata.conquery.resources.admin.ui.AdminUIResource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import io.dropwizard.auth.Auth;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.bakdata.conquery.resources.ResourceConstants.JOB_ID;

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
    public String executeScript(@Auth User user, String script) {
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
    public Object executeScriptJson(@Auth User user, String script) {
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
        return  processor.getJobs();
    }

    @GET
    @Path("logout")
    public Response logout() {
        return  Response.ok().cookie(AuthenticationConfig.expireAuthCookie()).build();
    }

}
