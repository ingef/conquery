package com.bakdata.conquery.resources.admin.rest;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.admin.ui.DatasetsUIResource;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HAdmin;
import io.dropwizard.views.View;
import lombok.Getter;
import lombok.Setter;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.List;
import java.util.stream.Collectors;

@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})

@Path("/datasets")
@Getter
@Setter
public class AdminDatasetsResource extends HAdmin {

	@Inject
	private AdminDatasetProcessor processor;
	@Inject
	private UIProcessor uiProcessor;

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response addDataset(@NotEmpty @FormDataParam("dataset_name") String name) throws JSONException {
		Dataset dataset = processor.addDataset(name);

		return Response.seeOther(UriBuilder.fromPath("/admin/")
										   .path(DatasetsUIResource.class)
										   .resolveTemplate(ResourceConstants.DATASET, dataset.getName())
										   .build())
					   .build();
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public View listDatasetsUI() {
		return new UIView<>("datasets.html.ftl", uiProcessor.getUIContext(), processor.getDatasetRegistry().getAllDatasets());
	}

	@GET
	public List<DatasetId> listDatasets() {
		return processor.getDatasetRegistry().getAllDatasets().stream().map(Dataset::getId).collect(Collectors.toList());
	}
}
