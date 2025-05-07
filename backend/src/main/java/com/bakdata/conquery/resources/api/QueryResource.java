package com.bakdata.conquery.resources.api;


import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
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

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.apiv1.MetaDataPatch;
import com.bakdata.conquery.apiv1.QueryProcessor;
import com.bakdata.conquery.apiv1.RequestAwareUriBuilder;
import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.statistics.ResultStatistics;
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
	public FullExecutionStatus getStatus(
			@Auth Subject subject,
			@PathParam(QUERY) ManagedExecutionId queryId,
			@QueryParam("all-providers") @DefaultValue("false") boolean allProviders) {

		return processor.getQueryFullStatus(queryId, subject, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders, true);
	}

	@GET
	@Path("{" + QUERY + "}/statistics")
	public ResultStatistics getDescription(@Auth Subject subject, @PathParam(QUERY) ManagedExecutionId queryId) {
		return processor.getResultStatistics(queryId, subject);
	}

	@PATCH
	@Path("{" + QUERY + "}")
	public FullExecutionStatus patchQuery(
			@Auth Subject subject,
			@PathParam(QUERY) ManagedExecutionId query,
			@QueryParam("all-providers") @DefaultValue("false") boolean allProviders,
			MetaDataPatch patch) {
		subject.authorize(query.getDataset(), Ability.READ);
		subject.authorize(query, Ability.READ);

		processor.patchQuery(subject, query, patch);

		return processor.getQueryFullStatus(query, subject, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders, false);
	}

	@DELETE
	@Path("{" + QUERY + "}")
	public void deleteQuery(@Auth Subject subject, @PathParam(QUERY) ManagedExecutionId query) {
		subject.authorize(query.getDataset(), Ability.READ);
		subject.authorize(query, Ability.DELETE);

		processor.deleteQuery(subject, query);
	}

	@POST
	@Path("{" + QUERY + "}/reexecute")
	public FullExecutionStatus reexecute(
			@Auth Subject subject,
			@PathParam(QUERY) ManagedExecutionId query,
			@QueryParam("all-providers") @DefaultValue("false") boolean allProviders) {
		subject.authorize(query.getDataset(), Ability.READ);
		subject.authorize(query, Ability.READ);

		processor.reexecute(subject, query);
		return processor.getQueryFullStatus(query, subject, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders, false);
	}

	@POST
	@Path("{" + QUERY + "}/cancel")
	public void cancel(@Auth Subject subject, @PathParam(QUERY) ManagedExecutionId query) {

		subject.authorize(query.getDataset(), Ability.READ);
		subject.authorize(query, Ability.CANCEL);

		processor.cancel(subject, query);
	}


}
