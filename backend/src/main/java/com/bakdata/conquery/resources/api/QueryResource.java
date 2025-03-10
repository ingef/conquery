package com.bakdata.conquery.resources.api;


import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.apiv1.MetaDataPatch;
import com.bakdata.conquery.apiv1.QueryProcessor;
import com.bakdata.conquery.apiv1.RequestAwareUriBuilder;
import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.SingleTableResult;
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

		processor.awaitDone(query, 1, TimeUnit.SECONDS);

		return processor.getQueryFullStatus(query, subject, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders);
	}

	@GET
	@Path("{" + QUERY + "}/statistics")
	public Response getDescription(@Auth Subject subject, @PathParam(QUERY) ManagedExecution query) {

		if (!(query instanceof SingleTableResult)) {
			throw new BadRequestException("Statistics is only available for %s".formatted(SingleTableResult.class.getSimpleName()));
		}

		subject.authorize(query.getDataset(), Ability.READ);
		subject.authorize(query, Ability.READ);

		if (processor.awaitDone(query, 1, TimeUnit.SECONDS) != ExecutionState.DONE) {
			return Response.status(Response.Status.CONFLICT.getStatusCode(), "Query is still running.").build(); // Request was submitted too early.
		}

		return Response.ok((processor.getResultStatistics(((ManagedExecution & SingleTableResult) query)))).build();
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
	public void deleteQuery(@Auth Subject subject, @PathParam(QUERY) ManagedExecution execution) {
		subject.authorize(execution.getDataset(), Ability.READ);
		subject.authorize(execution, Ability.DELETE);

		processor.deleteQuery(subject, execution.getId());
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

		processor.cancel(subject, query);
	}


}
