package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.apiv1.MetaDataPatch;
import com.bakdata.conquery.apiv1.RequestAwareUriBuilder;
import com.bakdata.conquery.apiv1.StoredQueriesProcessor;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.FullExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.resources.hierarchies.HDatasets;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;

@Path("datasets/{" + DATASET + "}/stored-queries")
@Consumes(AdditionalMediaTypes.JSON)
@Produces(AdditionalMediaTypes.JSON)

public class StoredQueriesResource extends HDatasets {

	@Inject
	private StoredQueriesProcessor processor;

	@Inject
	private MetaStorage storage;

	@GET
	public List<ExecutionStatus> getAllQueries(@PathParam(DATASET) Dataset dataset) {
		return processor.getAllQueries(getNamespace(), servletRequest, user)
						.collect(Collectors.toList());
	}

	@GET
	@Path("{" + QUERY + "}")
	public FullExecutionStatus getSingleQueryInfo(@PathParam(QUERY) ManagedExecution query) {
		return processor.getQueryFullStatus(query, user, RequestAwareUriBuilder.fromRequest(servletRequest));
	}

	@PATCH
	@Path("{" + QUERY + "}")
	public FullExecutionStatus patchQuery(@PathParam(QUERY) ManagedExecution query, MetaDataPatch patch) throws JSONException {
		processor.patchQuery(user, query, patch);
		
		return processor.getQueryFullStatus(query, user, RequestAwareUriBuilder.fromRequest(servletRequest));
	}

	@DELETE
	@Path("{" + QUERY + "}")
	public void deleteQuery(@PathParam(QUERY) ManagedExecution query) {
		processor.deleteQuery(query, user);
	}

	@POST
	@Path("{" + QUERY + "}/reexecute")
	public FullExecutionStatus reexecute(@Auth User user, @PathParam(DATASET) Dataset dataset, @PathParam(QUERY) ManagedExecution query, @Context HttpServletRequest req) {

		user.authorize(query, Ability.READ);

		return processor.reexecute(user, query, RequestAwareUriBuilder.fromRequest(req));
	}
}
