package com.bakdata.conquery.quarkus.api;

import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.quarkus.api.config.DatasetsRuntimeConfig;
import com.bakdata.conquery.quarkus.api.config.EntityPreviewRuntimeConfig;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/datasets")
@Produces(MediaType.APPLICATION_JSON)
public class DatasetsResource {
	@Inject
	DatasetsRuntimeConfig datasetsConfig;

	@Inject
	EntityPreviewRuntimeConfig entityPreviewConfig;

	@GET
	public List<DatasetResponse> getDatasets() {
		return datasetsConfig.datasets()
							 .stream()
							 .map(entry -> new DatasetResponse(entry.id(), entry.label()))
							 .toList();
	}

	@GET
	@Path("/{datasetId}/entity-preview")
	public EntityPreviewResponse getEntityPreview(@PathParam("datasetId") String datasetId) {
		boolean datasetExists = datasetsConfig.datasets().stream().anyMatch(dataset -> dataset.id().equals(datasetId));
		if (!datasetExists) {
			throw new NotFoundException("Unknown dataset: " + datasetId);
		}

		List<EntityPreviewResponse.LabeledSource> allSources =
				entityPreviewConfig.allSources().stream().map(source -> new EntityPreviewResponse.LabeledSource(source.name(), source.label())).toList();

		List<EntityPreviewResponse.LabeledSource> defaultSources =
				entityPreviewConfig.defaultSources().stream().map(source -> new EntityPreviewResponse.LabeledSource(source.name(), source.label())).toList();

		List<String> searchFilters = entityPreviewConfig.searchFilters()
											.map(value -> Stream.of(value.split(","))
																.map(String::trim)
																.filter(filter -> !filter.isEmpty())
																.toList())
											.orElse(List.of());
		String searchConcept = entityPreviewConfig.searchConcept().orElse(null);

		return new EntityPreviewResponse(allSources, defaultSources, searchFilters, searchConcept);
	}

	public record DatasetResponse(
			String id,
			String label
	) {
	}
}
