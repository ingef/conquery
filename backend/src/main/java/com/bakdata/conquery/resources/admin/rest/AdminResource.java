package com.bakdata.conquery.resources.admin.rest;

import com.bakdata.conquery.apiv1.FullExecutionStatus;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.jobs.JobManagerStatus;
import com.bakdata.conquery.models.messages.network.specific.CancelJobMessage;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.bakdata.conquery.resources.admin.ui.AdminUIResource;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.auth.Auth;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.bakdata.conquery.resources.ResourceConstants.JOB_ID;
import static org.apache.shiro.util.StringUtils.hasText;

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
        return processor.getJobs();
    }


    @GET
    @Path("/queries")
    public FullExecutionStatus[] getQueries(@Auth User currentUser) {
        final MetaStorage storage = processor.getStorage();
        final DatasetRegistry datasetRegistry = processor.getDatasetRegistry();
        return storage.getAllExecutions().stream()
                .map(t -> t.buildStatusFull(storage, currentUser, datasetRegistry))
                .filter(t -> t.getCreatedAt().toLocalDate().equals(LocalDate.now(t.getCreatedAt().getZone())))
                .limit(100)
                .toArray(FullExecutionStatus[]::new);
    }
}
