package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.apiv1.ExecutionStatus;
import com.bakdata.conquery.apiv1.FullExecutionStatus;
import com.bakdata.conquery.apiv1.QueryProcessor;
import com.bakdata.conquery.apiv1.RequestAwareUriBuilder;
import com.bakdata.conquery.apiv1.query.ExternalUpload;
import com.bakdata.conquery.apiv1.query.ExternalUploadResult;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.dropwizard.auth.Auth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Path("datasets/{" + DATASET + "}/queries")
@Data
@Consumes(AdditionalMediaTypes.JSON)
@Produces(AdditionalMediaTypes.JSON)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DatasetQueryResource {
	private final QueryProcessor processor;

	@Context
	protected HttpServletRequest servletRequest;

	@PathParam(DATASET)
	private Dataset dataset;


	@Data
	@AllArgsConstructor(onConstructor_ = {@JsonCreator})
	public static class EntityPreview {
		private String idKind; //TODO I think ID is fallback, but i dont currently know.
		private final String entityId;
		private final Range<LocalDate> time;
		@NsIdRefCollection
		private final List<Connector> sources;
	}

	@POST
	@Path("/entity")
	public FullExecutionStatus getEntityData(@Auth Subject subject, EntityPreview query, @Context HttpServletRequest request) {
		final UriBuilder uriBuilder = RequestAwareUriBuilder.fromRequest(request);
		return processor.getSingleEntityExport(subject, uriBuilder, query.getIdKind(), query.getEntityId(), query.getSources(), dataset, query.getTime());
	}

	@POST
	@Path("/upload")
	public ExternalUploadResult upload(@Auth Subject subject, @Valid ExternalUpload upload) {
		return processor.uploadEntities(subject, dataset, upload);
	}


	@GET
	public List<ExecutionStatus> getAllQueries(@Auth Subject subject, @QueryParam("all-providers") Optional<Boolean> allProviders) {

		subject.authorize(dataset, Ability.READ);

		return processor.getAllQueries(dataset, servletRequest, subject, allProviders.orElse(false)).collect(Collectors.toList());
	}

	@POST
	public Response postQuery(@Auth Subject subject, @QueryParam("all-providers") Optional<Boolean> allProviders, @NotNull @Valid QueryDescription query) {

		subject.authorize(dataset, Ability.READ);

		ManagedExecution<?> execution = processor.postQuery(dataset, query, subject, false);

		return Response.ok(processor.getQueryFullStatus(execution, subject, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders.orElse(false)))
					   .status(Response.Status.CREATED)
					   .build();
	}
}
