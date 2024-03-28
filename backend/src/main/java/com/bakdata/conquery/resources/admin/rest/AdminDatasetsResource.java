package com.bakdata.conquery.resources.admin.rest;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})

@Path("/datasets")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AdminDatasetsResource {

	private final AdminDatasetProcessor processor;

	@SneakyThrows
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
