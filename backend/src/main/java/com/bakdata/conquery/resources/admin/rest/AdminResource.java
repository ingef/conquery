package com.bakdata.conquery.resources.admin.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.admin.ui.DatasetsUIResource;
import com.bakdata.conquery.resources.hierarchies.HAdmin;
import lombok.Getter;
import lombok.Setter;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;

@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})

@Path("/")
@Getter
@Setter
public class AdminResource extends HAdmin {

	@Inject
	private AdminProcessor processor;

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Path("datasets")
	public Response addDataset(@NotEmpty @FormDataParam("dataset_name") String name) throws JSONException {
		Dataset dataset = processor.addDataset(name);

		return Response
					   .seeOther(UriBuilder.fromPath("/admin/").path(DatasetsUIResource.class).resolveTemplate(ResourceConstants.DATASET, name).build())
					   .build();
	}

	@GET
	@Path("datasets")
	public List<DatasetId> listDatasets() {
		return processor.getNamespaces().getAllDatasets().stream().map(Dataset::getId).collect(Collectors.toList());
	}
}
