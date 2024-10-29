package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.FORM_CONFIG;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import com.bakdata.conquery.apiv1.FormConfigPatch;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.forms.configs.FormConfig.FormConfigFullRepresentation;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormConfigProcessor;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
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
	public FormConfigFullRepresentation getConfig(@Auth Subject subject, @PathParam(FORM_CONFIG) FormConfigId form) {
		return processor.getConfig(subject, form);
	}
	
	@PATCH
	@Path("{" + FORM_CONFIG + "}")
	public FormConfigFullRepresentation patchConfig(@Auth Subject subject, @PathParam(FORM_CONFIG) FormConfigId form, FormConfigPatch patch ) {
		return processor.patchConfig(subject, form, patch);
	}
	
	@DELETE
	@Path("{" + FORM_CONFIG + "}")
	public void deleteConfig(@Auth Subject subject, @PathParam(FORM_CONFIG) FormConfigId form) {
		processor.deleteConfig(subject, form);
	}
	
}
