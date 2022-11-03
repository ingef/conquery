package com.bakdata.conquery.resources.admin.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})

@Path("/datasets")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AdminDatasetsResource {

	private final AdminDatasetProcessor processor;

	@POST
	@Consumes(ExtraMimeTypes.JSON_STRING)
	public void addDataset(@Valid @NotNull Dataset dataset) {
		processor.addDataset(dataset);
	}

	@GET
	public List<DatasetId> listDatasets() {
		return processor.getDatasetRegistry().getAllDatasets().stream().map(Dataset::getId).collect(Collectors.toList());
	}
}
