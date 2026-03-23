package com.bakdata.conquery.quarkus.api;

import java.util.List;

import com.bakdata.conquery.quarkus.api.config.DatasetsRuntimeConfig;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/datasets")
@Produces(MediaType.APPLICATION_JSON)
public class DatasetsResource {

	@Inject
	DatasetsRuntimeConfig datasetsConfig;

	@GET
	public List<DatasetResponse> getDatasets() {
		return datasetsConfig.datasets()
							 .stream()
							 .map(entry -> new DatasetResponse(entry.id(), entry.label()))
							 .toList();
	}

	public record DatasetResponse(
			String id,
			String label
	) {
	}
}
