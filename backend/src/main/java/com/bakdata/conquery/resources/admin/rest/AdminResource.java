package com.bakdata.conquery.resources.admin.rest;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.admin.ui.DatasetsUIResource;
import com.bakdata.conquery.resources.hierarchies.HAuthorized;

import lombok.Getter;
import lombok.Setter;

@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})

@Path("/")
@Getter @Setter
public class AdminResource extends HAuthorized {

	@Inject
	private AdminProcessor processor;
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Path("datasets")
	public Response addDataset(@NotEmpty @FormDataParam("dataset_name") String name) throws JSONException {
		Dataset dataset = processor.addDataset(name);
		user.addPermission(
			processor.getStorage(), DatasetPermission.onInstance(AbilitySets.DATASET_CREATOR, dataset.createId())
		);
		
		return Response
			.seeOther(UriBuilder.fromPath("/admin/").path(DatasetsUIResource.class).resolveTemplate(ResourceConstants.DATASET_NAME, name).build())
			.build();
	}
}
