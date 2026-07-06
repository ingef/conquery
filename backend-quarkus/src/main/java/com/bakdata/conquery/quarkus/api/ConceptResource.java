package com.bakdata.conquery.quarkus.api;

import java.util.*;

import com.bakdata.conquery.quarkus.services.DatasetService;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import com.bakdata.conquery.quarkus.util.ScopedId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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

@Path("/api/concepts")
@Produces(MediaType.APPLICATION_JSON)
public class ConceptResource {

	@Inject
	DatasetService datasetService;

	@GET
	@Path("/{conceptId}")
	@Operation(
			summary = "Get concept details",
			description = "Returns a concept map keyed by concept id, compatible with frontend tree loading."
	)
	public Map<String, ConceptNodeResponse> getConcept(@PathParam("conceptId") @NotBlank String conceptId) {

		DatasetCatalogRepository.Concept concept = datasetService.requireConcept(conceptId);
		String datasetId = ScopedId.extractDatasetId(conceptId)
								   .orElseThrow(() -> new IllegalStateException("Expected dataset-scoped concept id but got: " + conceptId));

		Map<String, ConceptNodeResponse> nodes = new HashMap<>();

		Map<String, DatasetCatalogRepository.ConceptElement> children = concept.children();

		// Add main node
		ConceptNodeResponse node = new ConceptNodeResponse(
				concept.label(),
				concept.description(),
				true,
				concept.childrenIds(),
				0L,
				0L,
				true,
				!children.isEmpty(),
				datasetService.listTablesForDataset(datasetId).stream().map(this::toTableResponse).toList(),
				List.of()
		);
		nodes.put(conceptId, node);

		// Add children
		children.forEach((id, child) -> {
            ConceptNodeResponse childNode = new ConceptNodeResponse(
                    child.label(),
                    child.description(),
                    true,
                    child.children(),
                    0L,
                    0L,
                    true,
                    !children.isEmpty(),
                    List.of(),
                    List.of()
            );
            nodes.put(id, childNode);
        });

		return nodes;
	}

	private TableResponse toTableResponse(DatasetCatalogRepository.TableRecord table) {
		List<ColumnResponse> columns = table.columns().stream().map(this::toColumnResponse).toList();
		List<FilterResponse> filters = columns.stream()
											  .map(column -> new FilterResponse(column.id(), column.label(), null, null, column.type().name()))
											  .toList();
		List<String> supportedSecondaryIds = table.columns().stream()
												 .map(DatasetCatalogRepository.ColumnRecord::secondaryId)
												 .filter(Objects::nonNull)
												 .distinct()
												 .toList();

		return new TableResponse(
				table.id(),
				tableDatasetId(table.id()),
				table.label(),
				false,
				true,
				filters,
				List.of(),
				columns,
				table.primaryColumn(),
				supportedSecondaryIds,
				null
		);
	}

	private ColumnResponse toColumnResponse(DatasetCatalogRepository.ColumnRecord column) {
		return new ColumnResponse(
				column.id(),
				column.label(),
				column.type(),
				column.secondaryId()
		);
	}

	private String tableDatasetId(String scopedId) {
		return ScopedId.extractDatasetId(scopedId)
					   .orElseThrow(() -> new IllegalStateException("Expected dataset-scoped table id but got: " + scopedId));
	}


	@POST
	@Path("/{conceptId}/resolve")
	@Operation(
			summary = "Resolve concept codes",
			description = "Resolves uploaded concept codes to concept ids for the same dataset as the requested root concept."
	)
	public ConceptResolveResponse resolveConceptCodes(
			@PathParam("conceptId") @NotBlank String conceptId,
			@Valid @NotNull ConceptCodeList payload
	) {
		DatasetService.ConceptCodeResolution resolution = datasetService.resolveConceptCodes(conceptId, payload.concepts);
		return new ConceptResolveResponse(resolution.resolvedConcepts(), resolution.unknownCodes());
	}

	public record ConceptNodeResponse(
			String label,
			String description,
			Boolean active,
			List<String> children,
			Long matchingEntries,
			Long matchingEntities,
			Boolean detailsAvailable,
			Boolean codeListResolvable,
			List<TableResponse> tables,
			List<SelectResponse> selects
	) {
	}

	public record TableResponse(
			String id,
			String connectorId,
			String label,
			Boolean exclude,
			@JsonProperty("default")
			Boolean defaultSelected,
			List<FilterResponse> filters,
			List<SelectResponse> selects,
			List<ColumnResponse> columns,
			String primaryColumn,
			List<String> supportedSecondaryIds,
			DateColumnResponse dateColumn
	) {
	}

	public record ColumnResponse(
			String id,
			String label,
			DatasetCatalogRepository.ColumnType type,
			String secondaryId
	) {
	}

	public record SelectResponse(
			String id,
			String label,
			String description,
			@JsonProperty("default")
			Boolean defaultSelected,
			SelectResultTypeResponse resultType
	) {
	}

	public record SelectResultTypeResponse(
			String type,
			ElementTypeResponse elementType
	) {
	}

	public record ElementTypeResponse(
			String type
	) {
	}

	public record FilterResponse(
			String id,
			String label,
			String description,
			String tooltip,
			String type
	) {
	}

	public record DateColumnResponse(
			List<ValueResponse> options,
			String defaultValue,
			String value,
			String tooltip
	) {
	}

	public record ValueResponse(
			String value,
			String label
	) {
	}

	public record ConceptResolveResponse(
			List<String> resolvedConcepts,
			List<String> unknownCodes
	) {
	}

	public static final class ConceptCodeList {
		public final @NotNull @NotEmpty List<@NotBlank String> concepts;

		@JsonCreator
		public ConceptCodeList(@JsonProperty("concepts") List<String> concepts) {
			this.concepts = concepts;
		}
	}
}
