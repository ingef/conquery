package com.bakdata.conquery.resources.api;


import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.apiv1.QueryProcessor;
import com.bakdata.conquery.apiv1.RequestAwareUriBuilder;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.FullExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

@Path("datasets/{" + DATASET + "}/queries")
@Consumes(AdditionalMediaTypes.JSON)
@Produces(AdditionalMediaTypes.JSON)
@Slf4j
public class QueryResource {

	@Inject
	private QueryProcessor processor;

	@Context
	protected HttpServletRequest servletRequest;

	@POST
	public Response postQuery(@Auth User user, @PathParam(DATASET) Dataset dataset, @QueryParam("all-providers") Optional<Boolean> allProviders, @NotNull @Valid QueryDescription query) {

		log.info("Query posted on dataset {} by user {} ({}).", dataset.getId(), user.getId(), user.getName());

		user.authorize(dataset, Ability.READ);

		ManagedExecution<?> execution = processor.postQuery(dataset, query, user);

		return Response.ok(processor.getQueryFullStatus(execution, user, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders.orElse(false)))
				.status(Status.CREATED)
				.build();
	}

	@DELETE
	@Path("{" + QUERY + "}")
	public void cancel(@Auth User user, @PathParam(DATASET) Dataset dataset, @PathParam(QUERY) ManagedExecution<?> query) {

		user.authorize(dataset, Ability.READ);
		user.authorize(query, Ability.CANCEL);

		processor.cancel(
				user,
				dataset,
				query
		);
	}

	@GET
	@Path("{" + QUERY + "}")
	public FullExecutionStatus getStatus(@Auth User user, @PathParam(DATASET) Dataset dataset, @PathParam(QUERY) ManagedExecution<?> query, @QueryParam("all-providers") Optional<Boolean> allProviders)
			throws InterruptedException {

		user.authorize(dataset, Ability.READ);
		user.authorize(query, Ability.READ);

		query.awaitDone(1, TimeUnit.SECONDS);



		return processor.getQueryFullStatus(query, user, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders.orElse(false));
	}
}
