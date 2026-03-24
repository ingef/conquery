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
import org.eclipse.microprofile.openapi.annotations.Operation;

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
		requireDataset(datasetId);

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

	@GET
	@Path("/{datasetId}/concepts")
	@Operation(
			summary = "Get root concepts for a dataset",
			description = "Nodes with detailsAvailable=false represent folder/structure nodes and should not be draggable. "
						  + "Their active flag should be false."
	)
	public ConceptsResponse getConcepts(@PathParam("datasetId") String datasetId) {
		DatasetsRuntimeConfig.DatasetEntry dataset = requireDataset(datasetId);

		ConceptsResponse.ConceptSummaryResponse rootConcept = new ConceptsResponse.ConceptSummaryResponse(
				dataset.label(),
				null,
				true,
				List.of(),
				0L,
				0L,
				false,
				false
		);

		return new ConceptsResponse(
				List.of(),
				java.util.Map.of(dataset.id(), rootConcept)
		);
	}

	private DatasetsRuntimeConfig.DatasetEntry requireDataset(String datasetId) {
		return datasetsConfig.datasets()
							 .stream()
							 .filter(dataset -> dataset.id().equals(datasetId))
							 .findFirst()
							 .orElseThrow(() -> new NotFoundException("Unknown dataset: " + datasetId));
	}

	public record DatasetResponse(
			String id,
			String label
	) {
	}
}
