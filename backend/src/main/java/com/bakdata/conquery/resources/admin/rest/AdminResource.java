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
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
        return storage.getAllExecutions().stream().map(t->t.buildStatusFull(storage, currentUser, datasetRegistry)).toArray(FullExecutionStatus[]::new);
  }



    // For test purposes
/*
    @GET
    @Path("/queries")
    public FullExecutionStatus[] getQueries() {
        CQAnd root = new CQAnd();
        CQConcept e1 = new CQConcept();
        e1.setLabel("Concept Label");
        root.setChildren(List.of(e1));
        Query testQuery = new ConceptQuery(root);

        FullExecutionStatus[] result = new FullExecutionStatus[]{
               new FullExecutionStatus(){
                    {
                        setId(new ManagedExecutionId(new DatasetId("Dataset2"),UUID.randomUUID()));
                        setOwnerName("Doriane");
                        setQueryType("QueryType1");
                        setStatus(ExecutionState.NEW);
                        setLabel("dataset2Label");
                        setCreatedAt(ZonedDateTime.now());
                        setStartTime(LocalDateTime.now().plusMinutes(3));
                        setFinishTime(LocalDateTime.now().plusMinutes(5));
                        setRequiredTime(3400L);
                        setProgress(0F);
                        setError(null);
                        setQuery(testQuery);
                    }
                },
                new FullExecutionStatus(){
                    {
                        setId(new ManagedExecutionId(new DatasetId("Dataset2"),UUID.randomUUID()));
                        setOwnerName("Doriane");
                        setQueryType("QueryType2");
                        setStatus(ExecutionState.RUNNING);
                        setLabel("dataset2Label");
                        setCreatedAt(ZonedDateTime.now());
                        setStartTime(LocalDateTime.now().plusMinutes(12));
                        setFinishTime(LocalDateTime.now().plusMinutes(33));
                        setRequiredTime(300L);
                        setProgress(78.9F);
                        setError(null);
                        setQuery(testQuery);
                    }
                },
                new FullExecutionStatus(){
                    {
                        setId(new ManagedExecutionId(new DatasetId("Dataset2"),UUID.randomUUID()));
                        setOwnerName("Doriane");
                        setQueryType("QueryType3");
                        setStatus(ExecutionState.FAILED);
                        setLabel("dataset2Label");
                        setCreatedAt(ZonedDateTime.now());
                        setStartTime(LocalDateTime.now().plusMinutes(1));
                        setFinishTime(null);
                        setRequiredTime(120L);
                        setProgress(12.6F);
                        setError(ConqueryError.asConqueryError(new IllegalStateException()));
                        setQuery(testQuery);
                    }
                },
                new FullExecutionStatus(){
                    {
                        setId(new ManagedExecutionId(new DatasetId("Dataset2"),UUID.randomUUID()));
                        setOwnerName("Doriane");
                        setQueryType("QueryType4");
                        setStatus(ExecutionState.DONE);
                        setLabel("dataset2Label");
                        setCreatedAt(ZonedDateTime.now());
                        setStartTime(LocalDateTime.now().plusMinutes(2));
                        setFinishTime(LocalDateTime.now().plusMinutes(3));
                        setRequiredTime(200L);
                        setProgress(99F);
                        setError(null);
                        setQuery(testQuery);
                    }
                },




                new FullExecutionStatus(){
                    {
                        setId(new ManagedExecutionId(new DatasetId("Dataset3"),UUID.randomUUID()));
                        setOwnerName("David");
                        setQueryType("QueryType1");
                        setStatus(ExecutionState.NEW);
                        setLabel("dataset3Label");
                        setCreatedAt(ZonedDateTime.now());
                        setStartTime(LocalDateTime.now().plusMinutes(2));
                        setFinishTime(LocalDateTime.now().plusMinutes(3));
                        setRequiredTime(200L);
                        setProgress(0F);
                        setError(null);
                        setQuery(testQuery);
                    }
                },
                new FullExecutionStatus(){
                    {
                        setId(new ManagedExecutionId(new DatasetId("Dataset3"),UUID.randomUUID()));
                        setOwnerName("David");
                        setQueryType("QueryType2");
                        setStatus(ExecutionState.RUNNING);
                        setLabel("dataset3Label");
                        setCreatedAt(ZonedDateTime.now());
                        setStartTime(LocalDateTime.now().plusMinutes(21));
                        setFinishTime(LocalDateTime.now().plusMinutes(32));
                        setRequiredTime(300L);
                        setProgress(43.1F);
                        setError(null);
                        setQuery(testQuery);
                    }
                },
                new FullExecutionStatus(){
                    {
                        setId(new ManagedExecutionId(new DatasetId("Dataset3"),UUID.randomUUID()));
                        setOwnerName("David");
                        setQueryType("QueryType3");
                        setStatus(ExecutionState.FAILED);
                        setLabel("dataset3Label");
                        setCreatedAt(ZonedDateTime.now());
                        setStartTime(LocalDateTime.now().plusMinutes(2));
                        setFinishTime(null);
                        setRequiredTime(120L);
                        setProgress(78.3F);
                        setError(ConqueryError.asConqueryError(new IllegalStateException()));
                        setQuery(testQuery);
                    }
                },
                new FullExecutionStatus(){
                    {
                        setId(new ManagedExecutionId(new DatasetId("Dataset3"),UUID.randomUUID()));
                        setOwnerName("David");
                        setQueryType("QueryType4");
                        setStatus(ExecutionState.DONE);
                        setLabel("dataset3Label");
                        setCreatedAt(ZonedDateTime.now());
                        setStartTime(LocalDateTime.now().plusMinutes(3));
                        setFinishTime(LocalDateTime.now().plusMinutes(9));
                        setRequiredTime(200L);
                        setProgress(99.9F);
                        setError(null);
                        setQuery(testQuery);
                    }
                },




                new FullExecutionStatus(){
                    {
                        setId(new ManagedExecutionId(new DatasetId("Dataset4"),UUID.randomUUID()));
                        setOwnerName("Meza");
                        setQueryType("QueryType1");
                        setStatus(ExecutionState.NEW);
                        setLabel("dataset4Label");
                        setCreatedAt(ZonedDateTime.now());
                        setStartTime(LocalDateTime.now().plusMinutes(9));
                        setFinishTime(LocalDateTime.now().plusMinutes(13));
                        setRequiredTime(2220L);
                        setProgress(0F);
                        setError(null);
                        setQuery(testQuery);
                    }
                },
                new FullExecutionStatus(){
                    {
                        setId(new ManagedExecutionId(new DatasetId("Dataset4"),UUID.randomUUID()));
                        setOwnerName("Meza");
                        setQueryType("QueryType2");
                        setStatus(ExecutionState.RUNNING);
                        setLabel("dataset4Label");
                        setCreatedAt(ZonedDateTime.now());
                        setStartTime(LocalDateTime.now().plusMinutes(9));
                        setFinishTime(LocalDateTime.now().plusMinutes(11));
                        setRequiredTime(330L);
                        setProgress(24.9F);
                        setError(null);
                        setQuery(testQuery);
                    }
                },
                new FullExecutionStatus(){
                    {
                        setId(new ManagedExecutionId(new DatasetId("Dataset4"),UUID.randomUUID()));
                        setOwnerName("Meza");
                        setQueryType("QueryType3");
                        setStatus(ExecutionState.FAILED);
                        setLabel("dataset4Label");
                        setCreatedAt(ZonedDateTime.now());
                        setStartTime(LocalDateTime.now().plusMinutes(23));
                        setFinishTime(null);
                        setRequiredTime(120L);
                        setProgress(55F);
                        setError(ConqueryError.asConqueryError(new IllegalStateException()));
                        setQuery(testQuery);
                    }
                },
                new FullExecutionStatus(){
                    {
                        setId(new ManagedExecutionId(new DatasetId("Dataset4"),UUID.randomUUID()));
                        setOwnerName("Meza");
                        setQueryType("QueryType4");
                        setStatus(ExecutionState.DONE);
                        setLabel("dataset4Label");
                        setCreatedAt(ZonedDateTime.now());
                        setStartTime(LocalDateTime.now().plusMinutes(17));
                        setFinishTime(LocalDateTime.now().plusMinutes(19));
                        setRequiredTime(200L);
                        setProgress(99F);
                        setError(null);
                        setQuery(testQuery);
                    }
                },

                new FullExecutionStatus(){
                    {
                        setId(new ManagedExecutionId(new DatasetId("Dataset1"),UUID.randomUUID()));
                        setOwnerName("Peter");
                        setQueryType("QueryType1");
                        setStatus(ExecutionState.NEW);
                        setLabel("dataset1Label");
                        setCreatedAt(ZonedDateTime.now());
                        setStartTime(LocalDateTime.now().plusMinutes(2));
                        setFinishTime(LocalDateTime.now().plusMinutes(3));
                        setRequiredTime(200L);
                        setProgress(0F);
                        setError(null);
                        setQuery(testQuery);
                    }
                },
                new FullExecutionStatus(){
                    {
                        setId(new ManagedExecutionId(new DatasetId("Dataset1"),UUID.randomUUID()));
                        setOwnerName("Audrane");
                        setQueryType("QueryType2");
                        setStatus(ExecutionState.RUNNING);
                        setLabel("dataset1Label");
                        setCreatedAt(ZonedDateTime.now());
                        setStartTime(LocalDateTime.now().plusMinutes(2));
                        setFinishTime(LocalDateTime.now().plusMinutes(3));
                        setRequiredTime(300L);
                        setProgress(35.45F);
                        setError(null);
                        setQuery(testQuery);
                    }
                },
                new FullExecutionStatus(){
                    {
                        setId(new ManagedExecutionId(new DatasetId("Dataset1"),UUID.randomUUID()));
                        setOwnerName("Audrane");
                        setQueryType("QueryType3");
                        setStatus(ExecutionState.FAILED);
                        setLabel("dataset1Label");
                        setCreatedAt(ZonedDateTime.now());
                        setStartTime(LocalDateTime.now().plusMinutes(2));
                        setFinishTime(null);
                        setRequiredTime(120L);
                        setProgress(35.45F);
                        setError(ConqueryError.asConqueryError(new IllegalStateException()));
                        setQuery(testQuery);
                    }
                },
                new FullExecutionStatus(){
                    {
                        setId(new ManagedExecutionId(new DatasetId("Dataset1"),UUID.randomUUID()));
                        setOwnerName("Audrane");
                        setQueryType("QueryType4");
                        setStatus(ExecutionState.DONE);
                        setLabel("dataset1Label");
                        setCreatedAt(ZonedDateTime.now());
                        setStartTime(LocalDateTime.now().plusMinutes(2));
                        setFinishTime(LocalDateTime.now().plusMinutes(3));
                        setRequiredTime(200L);
                        setProgress(99F);
                        setError(null);
                        setQuery(testQuery);
                    }
                },

        };

        return result;
    }
*/



}
