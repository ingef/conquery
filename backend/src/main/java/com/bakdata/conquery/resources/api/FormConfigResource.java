package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.FORM_CONFIG;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.FormConfigPatch;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.forms.configs.FormConfig.FormConfigFullRepresentation;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormConfigProcessor;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import lombok.RequiredArgsConstructor;

@Consumes(ExtraMimeTypes.JSON_STRING)
@Produces(ExtraMimeTypes.JSON_STRING)
@Path("form-configs")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class FormConfigResource {

	private final FormConfigProcessor processor;

	@GET
	@Path("{" + FORM_CONFIG + "}")
	public FormConfigFullRepresentation getConfig(@Auth Subject subject, @PathParam(FORM_CONFIG) FormConfig form) {
		return processor.getConfig(subject, form);
	}
	
	@PATCH
	@Path("{" + FORM_CONFIG + "}")
	public FormConfigFullRepresentation patchConfig(@Auth Subject subject, @PathParam(FORM_CONFIG) FormConfig form, FormConfigPatch patch ) {
		return processor.patchConfig(subject, form, patch);
	}
	
	@DELETE
	@Path("{" + FORM_CONFIG + "}")
	public Response deleteConfig(@Auth Subject subject, @PathParam(FORM_CONFIG) FormConfig form) {
		processor.deleteConfig(subject, form);
		return Response.ok().build();
	}
	
}
