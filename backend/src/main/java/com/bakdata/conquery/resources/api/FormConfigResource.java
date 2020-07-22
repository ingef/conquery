package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.FORM_CONFIG;

import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.apiv1.FormConfigPatch;
import com.bakdata.conquery.apiv1.forms.FormConfigAPI;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.forms.configs.FormConfig.FormConfigFullRepresentation;
import com.bakdata.conquery.models.forms.configs.FormConfig.FormConfigOverviewRepresentation;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormConfigProcessor;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormConfigProcessor.PostResponse;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;

@Consumes(ExtraMimeTypes.JSON_STRING)
@Produces(ExtraMimeTypes.JSON_STRING)
@Path("datasets/{" + DATASET + "}/form-configs")
public class FormConfigResource {
	
	@PathParam(DATASET)
	private DatasetId dataset;
	@Inject
	private FormConfigProcessor processor;
	
	@PathParam(DATASET)
	private DatasetId datasetId;
	
	@POST
	public Response postConfig(@Auth User user, @Valid FormConfigAPI config) {
		return Response.ok(new PostResponse(processor.addConfig(user, dataset, config))).status(Status.CREATED).build();
	}
	
	@GET
	public Stream<FormConfigOverviewRepresentation> getConfigByUserAndType(@Auth User user, @QueryParam("formType") Optional<String> formType) {
		return processor.getConfigsByFormType(user, dataset, formType);
	}

	@GET
	@Path("{" + FORM_CONFIG + "}")
	public FormConfigFullRepresentation getConfig(@Auth User user, @PathParam(FORM_CONFIG) FormConfigId formId) {
		return processor.getConfig(datasetId, user, formId);
	}
	
	@PATCH
	@Path("{" + FORM_CONFIG + "}")
	public FormConfigFullRepresentation patchConfig(@Auth User user, @PathParam(FORM_CONFIG) FormConfigId formId, FormConfigPatch patch ) {
		return processor.patchConfig(user, datasetId, formId, patch);
	}
	
	@DELETE
	@Path("{" + FORM_CONFIG + "}")
	public Response deleteConfig(@Auth User user, @PathParam(FORM_CONFIG) FormConfigId formId) {
		processor.deleteConfig(user, formId);
		return Response.ok().build();
	}
	
}
