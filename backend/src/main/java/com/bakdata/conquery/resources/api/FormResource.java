package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import java.util.Collection;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

@Path("datasets/{" + DATASET + "}/form-queries")
@Consumes(AdditionalMediaTypes.JSON)
@Produces(AdditionalMediaTypes.JSON)
@PermitAll
@Slf4j
public class FormResource {

	@Inject
	private FormProcessor processor;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<JsonNode> getFormFEConfigs(@Auth Subject subject) {
		return processor.getFormsForUser(subject);
	}

}
