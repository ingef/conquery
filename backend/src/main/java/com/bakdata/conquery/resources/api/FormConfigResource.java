package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.FORM_CONFIG;

import java.util.Optional;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.FormConfigProcessor;
import com.bakdata.conquery.apiv1.forms.FormConfig;
import com.bakdata.conquery.apiv1.forms.FormConfig.FormConfigFullRepresentation;
import com.bakdata.conquery.apiv1.forms.FormConfig.FormConfigOverviewRepresentation;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.resources.api.StoredQueriesResource.QueryPatch;
import com.bakdata.conquery.resources.hierarchies.HDatasets;
import io.dropwizard.jersey.PATCH;

@Consumes(ExtraMimeTypes.JSON_STRING)
@Produces(ExtraMimeTypes.JSON_STRING)
@Path("datasets/{" + DATASET + "}/form-configs")
public class FormConfigResource extends HDatasets {
	
	FormConfigProcessor processor;
	
	@POST
	public FormConfigId postConfig(FormConfig config) {
		return processor.addConfig(user, config);
	}
	
	@GET
	public Stream<FormConfigOverviewRepresentation> getConfigByUserAndType(@QueryParam("formType") Optional<String> formType) {
		return processor.getConfigsByFormType(user, formType);
	}

	@GET
	@Path("{" + FORM_CONFIG + "}")
	public FormConfigFullRepresentation getConfig(@PathParam(FORM_CONFIG) FormConfigId formId) {
		return processor.getConfig(datasetId, user, formId);
	}
	
	@PATCH
	@Path("{" + FORM_CONFIG + "}")
	public FormConfigFullRepresentation patchConfig(@PathParam(FORM_CONFIG) FormConfigId formId, QueryPatch patch ) {
		return processor.patchConfig(user, datasetId, formId, patch);
	}
	
	@DELETE
	@Path("{" + FORM_CONFIG + "}")
	public Response patchConfig(@PathParam(FORM_CONFIG) FormConfigId formId) {
		processor.deleteConfig(user, formId);
		return Response.ok().build();
	}
	
}
