package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import io.dropwizard.auth.Auth;
import io.dropwizard.validation.ValidationMethod;
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
	public FullExecutionStatus getEntityData(@Auth Subject subject, EntityPreviewRequest query, @Context HttpServletRequest request) {
		subject.authorize(dataset, Ability.READ);
		subject.authorize(dataset, Ability.PRESERVE_ID);

		final UriBuilder uriBuilder = RequestAwareUriBuilder.fromRequest(request);
		return processor.getSingleEntityExport(subject, uriBuilder, query.getIdKind(), query.getEntityId(), query.getSources(), dataset, query.getTime());
	}

	public static record ResolveEntitiesContainer(List<FilterValue<?>> filters){
		@ValidationMethod(message = "Only one Connector is supported.")
		public boolean isFiltersForSameConnector() {
			return filters().stream().map(fv -> fv.getFilter().getConnector()).distinct().count() == 1;
		}
	}


	@POST
	@Path("/resolve-entities")
	public Stream<Map<String, String>> resolveEntities(@Auth Subject subject, List<FilterValue<?>> container, @Context HttpServletRequest request) {
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
	public List<ExecutionStatus> getAllQueries(@Auth Subject subject, @QueryParam("all-providers") Optional<Boolean> allProviders) {

		subject.authorize(dataset, Ability.READ);

		return processor.getAllQueries(dataset, servletRequest, subject, allProviders.orElse(false)).collect(Collectors.toList());
	}

	@POST
	public Response postQuery(@Auth Subject subject, @QueryParam("all-providers") Optional<Boolean> allProviders, @NotNull @Valid QueryDescription query) {

		subject.authorize(dataset, Ability.READ);

		final ManagedExecution<?> execution = processor.postQuery(dataset, query, subject, false);

		return Response.ok(processor.getQueryFullStatus(execution, subject, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders.orElse(false)))
					   .status(Response.Status.CREATED)
					   .build();
	}
}
