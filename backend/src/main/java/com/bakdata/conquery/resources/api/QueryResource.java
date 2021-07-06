package com.bakdata.conquery.resources.api;


import com.bakdata.conquery.apiv1.*;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ManagedExecution;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

@Path("datasets/{" + DATASET + "}/queries")
@Consumes(AdditionalMediaTypes.JSON)
@Produces(AdditionalMediaTypes.JSON)
@Slf4j
public class QueryResource {

    @Inject
    private QueryProcessor processor;

    @Context
    protected HttpServletRequest servletRequest;

    @PathParam(DATASET)
    private Dataset dataset;

    @GET
    public List<ExecutionStatus> getAllQueries(@Auth User user, @QueryParam("all-providers") Optional<Boolean> allProviders) {

		user.authorize(dataset, Ability.READ);

		return processor.getAllQueries(dataset, servletRequest, user, allProviders.orElse(false))
				.collect(Collectors.toList());
    }

    @POST
    public Response postQuery(@Auth User user, @QueryParam("all-providers") Optional<Boolean> allProviders, @NotNull @Valid QueryDescription query) {

        user.authorize(dataset, Ability.READ);

        ManagedExecution<?> execution = processor.postQuery(dataset, query, user);

        return Response.ok(processor.getQueryFullStatus(execution, user, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders.orElse(false)))
                .status(Status.CREATED)
                .build();
    }

    @GET
    @Path("{" + QUERY + "}")
    public FullExecutionStatus getStatus(@Auth User user, @PathParam(QUERY) ManagedExecution<?> query, @QueryParam("all-providers") Optional<Boolean> allProviders)
            throws InterruptedException {

        user.authorize(dataset, Ability.READ);
        user.authorize(query, Ability.READ);

        query.awaitDone(1, TimeUnit.SECONDS);

        return processor.getQueryFullStatus(query, user, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders.orElse(false));
    }

    @PATCH
    @Path("{" + QUERY + "}")
    public FullExecutionStatus patchQuery(@Auth User user, @PathParam(QUERY) ManagedExecution<?> query, @QueryParam("all-providers") Optional<Boolean> allProviders, MetaDataPatch patch) throws JSONException {
        user.authorize(dataset, Ability.READ);
        user.authorize(query, Ability.READ);

        processor.patchQuery(user, query, patch);

        return processor.getQueryFullStatus(query, user, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders.orElse(false));
    }

    @DELETE
    @Path("{" + QUERY + "}")
    public void deleteQuery(@Auth User user, @PathParam(QUERY) ManagedExecution<?> query) {
        user.authorize(dataset, Ability.READ);
        user.authorize(query, Ability.DELETE);

        processor.deleteQuery(user, query);
    }

    @POST
    @Path("{" + QUERY + "}/reexecute")
    public FullExecutionStatus reexecute(@Auth User user, @PathParam(QUERY) ManagedExecution<?> query, @QueryParam("all-providers") Optional<Boolean> allProviders) {
        user.authorize(dataset, Ability.READ);
        user.authorize(query, Ability.READ);

        processor.reexecute(user, query);
        return processor.getQueryFullStatus(query, user, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders.orElse(false));
    }

    @POST
    @Path("{" + QUERY + "}/cancel")
    public void cancel(@Auth User user, @PathParam(QUERY) ManagedExecution<?> query) {

        user.authorize(dataset, Ability.READ);
        user.authorize(query, Ability.CANCEL);

        processor.cancel(
                user,
                dataset,
                query
        );
    }
}
