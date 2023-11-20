package com.bakdata.conquery.resources.api;


import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.apiv1.MetaDataPatch;
import com.bakdata.conquery.apiv1.QueryProcessor;
import com.bakdata.conquery.apiv1.RequestAwareUriBuilder;
import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.ManagedQuery;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import lombok.RequiredArgsConstructor;

@Path("queries")
@Consumes(AdditionalMediaTypes.JSON)
@Produces(AdditionalMediaTypes.JSON)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class QueryResource {

	private final QueryProcessor processor;

	@Context
	protected HttpServletRequest servletRequest;


	@GET
	@Path("{" + QUERY + "}")
	public FullExecutionStatus getStatus(@Auth Subject subject, @PathParam(QUERY) ManagedExecution query, @QueryParam("all-providers") @DefaultValue("false") boolean allProviders) {

		subject.authorize(query.getDataset(), Ability.READ);
		subject.authorize(query, Ability.READ);

		query.awaitDone(1, TimeUnit.SECONDS);

		return processor.getQueryFullStatus(query, subject, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders);
	}

	@GET
	@Path("{" + QUERY + "}/statistics")
	public Response getDescription(@Auth Subject subject, @PathParam(QUERY) ManagedExecution query) {

		if (!(query instanceof ManagedQuery)) {
			throw new BadRequestException("Statistics is only available for %s".formatted(ManagedQuery.class.getSimpleName()));
		}

		subject.authorize(query.getDataset(), Ability.READ);
		subject.authorize(query, Ability.READ);

		if(query.awaitDone(1, TimeUnit.SECONDS) != ExecutionState.DONE){
			return Response.status(Response.Status.CONFLICT).build(); // Request was submitted too early.
		}

		return Response.ok((processor.getResultStatistics(((ManagedQuery) query)))).build();
	}

	@PATCH
	@Path("{" + QUERY + "}")
	public FullExecutionStatus patchQuery(@Auth Subject subject, @PathParam(QUERY) ManagedExecution query, @QueryParam("all-providers") @DefaultValue("false") boolean allProviders, MetaDataPatch patch) {
		subject.authorize(query.getDataset(), Ability.READ);
		subject.authorize(query, Ability.READ);

		processor.patchQuery(subject, query, patch);

		return processor.getQueryFullStatus(query, subject, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders);
	}

	@DELETE
	@Path("{" + QUERY + "}")
	public void deleteQuery(@Auth Subject subject, @PathParam(QUERY) ManagedExecution query) {
		subject.authorize(query.getDataset(), Ability.READ);
		subject.authorize(query, Ability.DELETE);

		processor.deleteQuery(subject, query);
	}

	@POST
	@Path("{" + QUERY + "}/reexecute")
	public FullExecutionStatus reexecute(@Auth Subject subject, @PathParam(QUERY) ManagedExecution query, @QueryParam("all-providers") @DefaultValue("false") boolean allProviders) {
		subject.authorize(query.getDataset(), Ability.READ);
		subject.authorize(query, Ability.READ);

		processor.reexecute(subject, query);
		return processor.getQueryFullStatus(query, subject, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders);
	}

	@POST
	@Path("{" + QUERY + "}/cancel")
	public void cancel(@Auth Subject subject, @PathParam(QUERY) ManagedExecution query) {

		subject.authorize(query.getDataset(), Ability.READ);
		subject.authorize(query, Ability.CANCEL);

		processor.cancel(subject, query.getDataset(), query);
	}


}
