package com.bakdata.conquery.resources.admin.rest;

import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
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
	public void addDataset(@NotNull @Valid Dataset dataset) {
		processor.addDataset(dataset);
	}

	@GET
	public Stream<DatasetId> listDatasets() {
		return processor.getDatasetRegistry().getAllDatasets();
	}
}
