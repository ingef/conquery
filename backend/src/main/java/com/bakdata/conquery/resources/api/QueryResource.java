package com.bakdata.conquery.resources.api;


import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.apiv1.MetaDataPatch;
import com.bakdata.conquery.apiv1.QueryProcessor;
import com.bakdata.conquery.apiv1.RequestAwareUriBuilder;
import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.statistics.ResultStatistics;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import lombok.RequiredArgsConstructor;

import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

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

		subject.authorize(query.getDataset().resolve(), Ability.READ);
		subject.authorize(query, Ability.READ);

		return processor.getQueryFullStatus(query, subject, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders);
	}

	@GET
	@Path("{" + QUERY + "}/statistics")
	public ResultStatistics getDescription(@Auth Subject subject, @PathParam(QUERY) ManagedExecution query) {

		if (!(query instanceof SingleTableResult)) {
			throw new BadRequestException("Statistics is only available for %s".formatted(SingleTableResult.class.getSimpleName()));
		}

		subject.authorize(query.getDataset().resolve(), Ability.READ);
		subject.authorize(query, Ability.READ);


		return processor.getResultStatistics(((ManagedExecution & SingleTableResult) query));
	}

	@PATCH
	@Path("{" + QUERY + "}")
	public FullExecutionStatus patchQuery(@Auth Subject subject, @PathParam(QUERY) ManagedExecution query, @QueryParam("all-providers") @DefaultValue("false") boolean allProviders, MetaDataPatch patch) {
		subject.authorize(query.getDataset().resolve(), Ability.READ);
		subject.authorize(query, Ability.READ);

		processor.patchQuery(subject, query, patch);

		return processor.getQueryFullStatus(query, subject, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders);
	}

	@DELETE
	@Path("{" + QUERY + "}")
	public void deleteQuery(@Auth Subject subject, @PathParam(QUERY) ManagedExecution query) {
		subject.authorize(query.getDataset().resolve(), Ability.READ);
		subject.authorize(query, Ability.DELETE);

		processor.deleteQuery(subject, query);
	}

	@POST
	@Path("{" + QUERY + "}/reexecute")
	public FullExecutionStatus reexecute(@Auth Subject subject, @PathParam(QUERY) ManagedExecution query, @QueryParam("all-providers") @DefaultValue("false") boolean allProviders) {
		subject.authorize(query.getDataset().resolve(), Ability.READ);
		subject.authorize(query, Ability.READ);

		processor.reexecute(subject, query);
		return processor.getQueryFullStatus(query, subject, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders);
	}

	@POST
	@Path("{" + QUERY + "}/cancel")
	public void cancel(@Auth Subject subject, @PathParam(QUERY) ManagedExecution query) {

		subject.authorize(query.getDataset().resolve(), Ability.READ);
		subject.authorize(query, Ability.CANCEL);

		processor.cancel(subject, query.getDataset().resolve(), query);
	}


}
