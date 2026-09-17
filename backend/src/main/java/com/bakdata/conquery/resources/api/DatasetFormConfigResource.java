package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import java.util.Set;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.bakdata.conquery.apiv1.forms.FormConfigAPI;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.forms.configs.FormConfig.FormConfigOverviewRepresentation;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormConfigProcessor;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormConfigProcessor.PostResponse;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.resources.hierarchies.HAuthorized;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Consumes(ExtraMimeTypes.JSON_STRING)
@Produces(ExtraMimeTypes.JSON_STRING)
@Path("datasets/{" + DATASET + "}/form-configs")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@ToString
public class DatasetFormConfigResource extends HAuthorized {

	private final FormConfigProcessor processor;
	@PathParam(DATASET)
	private DatasetId dataset;

	@POST
	public Response postConfig(@Auth Subject subject, @Valid FormConfigAPI config) {
		subject.authorize(dataset, Ability.READ);

		return Response.ok(new PostResponse(processor.addConfig(subject, dataset, config).getId())).status(Status.CREATED).build();
	}

	@GET
	public Stream<FormConfigOverviewRepresentation> getConfigByUserAndType(@Auth Subject subject, @QueryParam("formType") Set<String> formType) {
		subject.authorize(dataset, Ability.READ);

		return processor.getConfigsByFormType(subject, dataset, formType);
	}


}
