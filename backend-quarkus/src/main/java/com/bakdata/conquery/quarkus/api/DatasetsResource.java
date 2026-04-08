package com.bakdata.conquery.quarkus.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.quarkus.api.config.EntityPreviewRuntimeConfig;
import com.bakdata.conquery.quarkus.api.config.FormQueriesRuntimeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;

@Path("/api/datasets")
@Produces(MediaType.APPLICATION_JSON)
public class DatasetsResource {
	@Inject
	DatasetService datasetService;

	@Inject
	EntityPreviewRuntimeConfig entityPreviewConfig;

	@Inject
	FormQueriesRuntimeConfig formQueriesConfig;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	QueryStateService queryStateService;

	@Inject
	QueryUploadService queryUploadService;

	@Inject
	Instance<SecurityIdentity> identity;

	@GET
	public List<DatasetResponse> getDatasets() {
		return datasetService.listDatasets().stream()
							 .map(entry -> new DatasetResponse(entry.id(), entry.label()))
							 .toList();
	}

	@GET
	@Path("/{datasetId}/entity-preview")
	public EntityPreviewResponse getEntityPreview(@PathParam("datasetId") String datasetId) {
		datasetService.requireDataset(datasetId);

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
		datasetService.requireDataset(datasetId);

		java.util.Map<String, ConceptsResponse.ConceptSummaryResponse> concepts = new LinkedHashMap<>();
		datasetService.listConceptsForDataset(datasetId).forEach(entry -> concepts.put(
				entry.id(),
				new ConceptsResponse.ConceptSummaryResponse(
						entry.label(),
						null,
						true,
						List.of(),
						0L,
						0L,
						true,
						false,
						List.of(),
						List.of()
				)
		));

		return new ConceptsResponse(
				List.of(),
				concepts
		);
	}

	@GET
	@Path("/{datasetId}/form-queries")
	@Operation(
			summary = "Get form configurations for a dataset",
			description = "Returns raw frontend form configuration objects."
	)
	public List<Object> getFormQueries(@PathParam("datasetId") String datasetId) {
		datasetService.requireDataset(datasetId);
		return formQueriesConfig.resources().stream().map(this::loadFormResource).toList();
	}

	@GET
	@Path("/{datasetId}/queries")
	@Operation(
			summary = "List queries for a dataset",
			description = "Returns the query history list for the given dataset."
	)
	public List<QuerySummaryResponse> getQueries(@PathParam("datasetId") String datasetId) {
		datasetService.requireDataset(datasetId);
		return queryStateService.getDatasetQueries(datasetId);
	}

	@POST
	@Path("/{datasetId}/queries")
	@Operation(
			summary = "Create a query for a dataset",
			description = "Accepts a query payload and returns the created query id."
	)
	public StartQueryResponse postQueries(@PathParam("datasetId") String datasetId, QuerySubmissionPayload payload) {
		datasetService.requireDataset(datasetId);
		return queryStateService.createQuery(datasetId, payload, resolveUserName(identity));
	}

	@POST
	@Path("/{datasetId}/queries/upload")
	@Operation(
			summary = "Upload query entities",
			description = "Uploads entity id rows for query upload workflow."
	)
	public UploadQueryResponse uploadQueries(
			@PathParam("datasetId") String datasetId,
			@Valid @NotNull QueryUploadPayload payload
	) {
		datasetService.requireDataset(datasetId);
		QueryUploadService.UploadResult result = queryUploadService.processUpload(
				new QueryUploadService.QueryUploadPayload(payload.format, payload.values, payload.label)
		);
		return new UploadQueryResponse(result.resolved(), result.unresolvedId(), result.unreadableDate());
	}

	private static String resolveUserName(Instance<SecurityIdentity> identityInstance) {
		if (identityInstance.isResolvable()) {
			SecurityIdentity securityIdentity = identityInstance.get();
			if (securityIdentity != null && !securityIdentity.isAnonymous() && securityIdentity.getPrincipal() != null) {
				String principalName = securityIdentity.getPrincipal().getName();
				if (principalName != null && !principalName.isBlank()) {
					return principalName;
				}
			}
		}

		return "anonymous";
	}

	private Object loadFormResource(String path) {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		try (InputStream input = classLoader.getResourceAsStream(path)) {
			if (input == null) {
				throw new IllegalStateException("Configured form resource does not exist: " + path);
			}
			return objectMapper.readValue(input, Object.class);
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
			List<QueryResource.ResultUrlResponse> resultUrls,
			boolean shared,
			boolean canExpand,
			String queryType,
			String secondaryId,
			boolean containsDates
	) {
	}

	public record StartQueryResponse(
			String id
	) {
	}

	public record UploadQueryResponse(
			int resolved,
			List<List<String>> unresolvedId,
			List<List<String>> unreadableDate
	) {
	}

	public static final class QueryUploadPayload {
		public final @NotNull @NotEmpty List<@NotBlank String> format;
		public final @NotNull List<@NotNull List<@NotBlank String>> values;
		public final @NotBlank String label;

		public QueryUploadPayload(List<String> format, List<List<String>> values, String label) {
			this.format = format;
			this.values = values;
			this.label = label;
		}
	}

	public record DatasetResponse(
			String id,
			String label
	) {
	}
}
