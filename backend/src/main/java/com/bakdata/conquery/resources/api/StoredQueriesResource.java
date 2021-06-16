package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.apiv1.MetaDataPatch;
import com.bakdata.conquery.apiv1.QueryProcessor;
import com.bakdata.conquery.apiv1.RequestAwareUriBuilder;
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
	private QueryProcessor processor;

	@GET
	public List<ExecutionStatus> getAllQueries(@PathParam(DATASET) Dataset dataset, @QueryParam("all-providers") Optional<Boolean> allProviders) {
		return processor.getAllQueries(getNamespace(), servletRequest, user, allProviders.orElse(false))
						.collect(Collectors.toList());
	}

	@GET
	@Path("{" + QUERY + "}")
	public FullExecutionStatus getSingleQueryInfo(@PathParam(QUERY) ManagedExecution<?> query, @QueryParam("all-providers") Optional<Boolean> allProviders) {

		user.authorize(getDataset(), Ability.READ);
		user.authorize(query, Ability.READ);

		return processor.getQueryFullStatus(query, user, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders.orElse(false));
	}

	@PATCH
	@Path("{" + QUERY + "}")
	public FullExecutionStatus patchQuery(@PathParam(QUERY) ManagedExecution<?> query, @QueryParam("all-providers") Optional<Boolean> allProviders, MetaDataPatch patch) throws JSONException {
		user.authorize(getDataset(), Ability.READ);
		user.authorize(query, Ability.READ);

		processor.patchQuery(user, query, patch);
		
		return processor.getQueryFullStatus(query, user, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders.orElse(false));
	}

	@DELETE
	@Path("{" + QUERY + "}")
	public void deleteQuery(@PathParam(QUERY) ManagedExecution<?> query) {
		user.authorize(getDataset(), Ability.READ);
		user.authorize(query, Ability.READ);
		user.authorize(query, Ability.DELETE);

		processor.deleteQuery(user, query);
	}

	@POST
	@Path("{" + QUERY + "}/reexecute")
	public FullExecutionStatus reexecute(@Auth User user, @PathParam(DATASET) Dataset dataset, @PathParam(QUERY) ManagedExecution<?> query, @QueryParam("all-providers") Optional<Boolean> allProviders) {
		user.authorize(dataset, Ability.READ);
		user.authorize(query, Ability.READ);

		processor.reexecute(user, query);
		return processor.getQueryFullStatus(query, user, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders.orElse(false));
	}
}
