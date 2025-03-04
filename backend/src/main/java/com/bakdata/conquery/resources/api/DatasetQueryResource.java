package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.apiv1.QueryProcessor;
import com.bakdata.conquery.apiv1.RequestAwareUriBuilder;
import com.bakdata.conquery.apiv1.execution.ExecutionStatus;
import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.apiv1.query.ExternalUpload;
import com.bakdata.conquery.apiv1.query.ExternalUploadResult;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import io.dropwizard.auth.Auth;
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


	@POST
	@Path("/entity")
	public FullExecutionStatus getEntityData(@Auth Subject subject, @Valid EntityPreviewRequest query, @Context HttpServletRequest request) {
		subject.authorize(dataset, Ability.READ);
		subject.authorize(dataset, Ability.PRESERVE_ID);

		final UriBuilder uriBuilder = RequestAwareUriBuilder.fromRequest(request);
		return processor.getSingleEntityExport(subject, uriBuilder, query.getIdKind(), query.getEntityId(), query.getSources(), dataset, query.getTime());
	}


	@POST
	@Path("/resolve-entities")
	public Stream<Map<String, String>> resolveEntities(@Auth Subject subject, @Valid @NotEmpty List<FilterValue<?>> container) {
		subject.authorize(dataset, Ability.READ);
		subject.authorize(dataset, Ability.PRESERVE_ID);

		return processor.resolveEntities(subject, container, dataset);
	}



	@POST
	@Path("/upload")
	public ExternalUploadResult upload(@Auth Subject subject, @Valid ExternalUpload upload) {
		subject.authorize(dataset, Ability.READ);
		subject.authorize(dataset, Ability.PRESERVE_ID);

		return processor.uploadEntities(subject, dataset, upload);
	}


	@GET
	public List<? extends ExecutionStatus> getAllQueries(@Auth Subject subject, @QueryParam("all-providers") Optional<Boolean> allProviders) {

		subject.authorize(dataset, Ability.READ);

		return processor.getAllQueries(dataset, servletRequest, subject, allProviders.orElse(false));
	}

	@POST
	public Response postQuery(@Auth Subject subject, @QueryParam("all-providers") Optional<Boolean> allProviders, @NotNull @Valid QueryDescription query) {

		subject.authorize(dataset, Ability.READ);

		final ManagedExecution execution = processor.postQuery(dataset, query, subject, false);

		return Response.ok(processor.getQueryFullStatus(execution, subject, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders.orElse(false)))
					   .status(Response.Status.CREATED)
					   .build();
	}
}
