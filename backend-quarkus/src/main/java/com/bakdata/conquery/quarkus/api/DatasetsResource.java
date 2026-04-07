package com.bakdata.conquery.quarkus.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.quarkus.api.config.DatasetsRuntimeConfig;
import com.bakdata.conquery.quarkus.api.config.EntityPreviewRuntimeConfig;
import com.bakdata.conquery.quarkus.api.config.FormQueriesRuntimeConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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

	@Inject
	FormQueriesRuntimeConfig formQueriesConfig;

	@Inject
	ObjectMapper objectMapper;

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
			description = "Returns top-level concept nodes. Nodes with detailsAvailable=false represent folder/structure nodes."
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
				true,
				false
		);

		return new ConceptsResponse(
				List.of(),
				java.util.Map.of(dataset.id(), rootConcept)
		);
	}

	@GET
	@Path("/{datasetId}/form-queries")
	@Operation(
			summary = "Get form configurations for a dataset",
			description = "Returns raw frontend form configuration objects."
	)
	public List<JsonNode> getFormQueries(@PathParam("datasetId") String datasetId) {
		requireDataset(datasetId);
		return formQueriesConfig.resources().stream().map(this::loadFormResource).toList();
	}

	@GET
	@Path("/{datasetId}/queries")
	@Operation(
			summary = "List queries for a dataset",
			description = "Returns the query history list for the given dataset."
	)
	public List<QuerySummaryResponse> getQueries(@PathParam("datasetId") String datasetId) {
		requireDataset(datasetId);
		return List.of();
	}

	@POST
	@Path("/{datasetId}/queries")
	@Operation(
			summary = "Create a query for a dataset",
			description = "Accepts a query payload and returns the created query id."
	)
	public StartQueryResponse postQueries(@PathParam("datasetId") String datasetId, QuerySubmissionPayload payload) {
		requireDataset(datasetId);
		return new StartQueryResponse(java.util.UUID.randomUUID().toString());
	}

	private DatasetsRuntimeConfig.DatasetEntry requireDataset(String datasetId) {
		return datasetsConfig.datasets()
							 .stream()
							 .filter(dataset -> dataset.id().equals(datasetId))
							 .findFirst()
							 .orElseThrow(() -> new NotFoundException("Unknown dataset: " + datasetId));
	}

	private JsonNode loadFormResource(String path) {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		try (InputStream input = classLoader.getResourceAsStream(path)) {
			if (input == null) {
				throw new IllegalStateException("Configured form resource does not exist: " + path);
			}
			return objectMapper.readTree(input);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Failed to parse form resource: " + path, e);
		}
	}

	public record QuerySummaryResponse(
			String id,
			String label,
			Long numberOfResults,
			String createdAt,
			List<String> tags,
			boolean own,
			String ownerName,
			boolean system,
			List<ResultUrlResponse> resultUrls,
			boolean shared,
			boolean canExpand,
			String queryType,
			String secondaryId,
			boolean containsDates
	) {
	}

	public record ResultUrlResponse(
			String label,
			String url
	) {
	}

	public record StartQueryResponse(
			String id
	) {
	}

	public record DatasetResponse(
			String id,
			String label
	) {
	}
}
