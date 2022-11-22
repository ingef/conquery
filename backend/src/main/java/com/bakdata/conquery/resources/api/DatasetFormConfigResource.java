package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.apiv1.forms.FormConfigAPI;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.forms.configs.FormConfig.FormConfigOverviewRepresentation;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormConfigProcessor;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormConfigProcessor.PostResponse;
import com.bakdata.conquery.resources.hierarchies.HAuthorized;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;

@Consumes(ExtraMimeTypes.JSON_STRING)
@Produces(ExtraMimeTypes.JSON_STRING)
@Path("datasets/{" + DATASET + "}/form-configs")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DatasetFormConfigResource extends HAuthorized {

	private final FormConfigProcessor processor;
	@PathParam(DATASET)
	private Dataset dataset;

	@POST
	public Response postConfig(@Auth Subject subject, @Valid FormConfigAPI config) {
		return Response.ok(new PostResponse(processor.addConfig(subject, dataset, config).getId())).status(Status.CREATED).build();
	}

	@GET
	public Stream<FormConfigOverviewRepresentation> getConfigByUserAndType(@Auth Subject subject, @QueryParam("formType") Set<String> formType) {
		return processor.getConfigsByFormType(subject, dataset, formType);
	}


}
