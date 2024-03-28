package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import java.util.Collection;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.auth.Auth;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Path("datasets/{" + DATASET + "}/form-queries")
@Consumes(AdditionalMediaTypes.JSON)
@Produces(AdditionalMediaTypes.JSON)
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DatasetFormResource {

	private final FormProcessor processor;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<JsonNode> getFormFEConfigs(@Auth Subject subject) {
		return processor.getFormsForUser(subject);
	}

}
