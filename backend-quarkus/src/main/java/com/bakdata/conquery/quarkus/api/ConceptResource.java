package com.bakdata.conquery.quarkus.api;

import java.util.*;

import com.bakdata.conquery.quarkus.ids.ConceptId;
import com.bakdata.conquery.quarkus.services.DatasetService;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.smallrye.common.constraint.Nullable;
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
		ConceptId parsedConceptId = ConceptId.parse(conceptId);

		Map<String, ConceptNodeResponse> nodes = new HashMap<>();

		Map<ConceptId, DatasetCatalogRepository.ConceptElement> children = concept.children();

		// Add main node
		ConceptNodeResponse node = new ConceptNodeResponse(concept.label(), concept.description(), true, concept.childrenIds().stream().map(ConceptId::toString).toList(), 0L, 0L, true, !children.isEmpty(), concept.connectors().stream().map(this::toConnectorResponse).toList(), List.of());
		nodes.put(parsedConceptId.toString(), node);

		// Add children
		children.forEach((id, child) -> {
			ConceptNodeResponse childNode = new ConceptNodeResponse(child.label(), child.description(), true, child.children().stream().map(ConceptId::toString).toList(), 0L, 0L, true, !children.isEmpty(), List.of(), List.of());
			nodes.put(id.toString(), childNode);
		});

		return nodes;
	}

	private ConnectorResponse toConnectorResponse(DatasetCatalogRepository.Connector connector) {
		DatasetCatalogRepository.TableRecord tableRecord = datasetService.requireTable(connector.tableId());
		List<String> supportedSecondaryIds = tableRecord.columns().stream()
				.map(DatasetCatalogRepository.ColumnRecord::secondaryId)
				.filter(Objects::nonNull)
				.distinct()
				.toList();
		return new ConnectorResponse(
				connector.tableId().toString(),
				connector.id().toString(),
				connector.label(),
				connector.isDefault(),
				connector.filters().stream().map(this::toFilterResponse).toList(),
				List.of(),
				supportedSecondaryIds
		);
	}

	private FilterResponse toFilterResponse(DatasetCatalogRepository.Filter filter) {
		return new FilterResponse(
				filter.id().toString(),
				filter.label(),
				filter.type(),
				filter.unit(),
				filter.tooltip(),
				filter.options().stream()
						.map(option -> new FrontendValue(option.value(), option.label(), option.optionValue()))
						.toList(),
				filter.min(),
				filter.max(),
				filter.pattern(),
				filter.allowDropFile(),
				filter.creatable(),
				filter.defaultValue()
		);
	}


	@POST
	@Path("/{conceptId}/resolve")
	@Operation(summary = "Resolve concept codes", description = "Resolves uploaded concept codes to concept ids for the same dataset as the requested root concept.")
	public ConceptResolveResponse resolveConceptCodes(@PathParam("conceptId") @NotBlank String conceptId, @Valid @NotNull ConceptCodeList payload) {
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
			List<ConnectorResponse> tables,
			List<SelectResponse> selects
	) {
	}

	public record ConnectorResponse(
			String id,
			String connectorId,
			String label,
			@JsonProperty("default") Boolean isDefault,
			List<FilterResponse> filters,
			List<SelectResponse> selects,
			List<String> supportedSecondaryIds
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
			@JsonProperty("default") Boolean defaultSelected,
			SelectResultTypeResponse resultType
	) {
	}

	public record SelectResultTypeResponse(
			String type,
			ElementTypeResponse elementType
	) {
	}

	public record ElementTypeResponse(String type) {
	}

	public record FilterResponse(

			@NotNull String id,
			/**
			 * User readable name of the Filter.
			 */
			@NotEmpty String label,
			/**
			 * Kind of filter: Communicates to the frontend which UI element to use and what values are valid.
			 */
			@NotEmpty String type,
			/**
			 * Used as display unit for enumerations etc in UI elements.
			 */
			String unit,

			/**
			 * Displayed on hover for filters.
			 */
			String tooltip,

			List<FrontendValue> options,

			/**
			 * min value for range filters.
			 */
			Integer min,
			/**
			 * max value for range filters.
			 */
			Integer max,

			String pattern,
			/**
			 * If true, enables users to use drag and drop files into the filter element (usually for {@link com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter}).
			 */
			boolean allowDropFile,
			/**
			 * If true, user can manually insert their input. At the moment only true for SelectFilter without any enabled backing searches.
			 */
			boolean creatable,
			/**
			 * If set, default value used for the filter by the frontend.
			 */
			@Nullable Object defaultValue
	) {
	}

	record FrontendValue(String value, String label, String optionValue) {
	}

	public record DateColumnResponse(List<ValueResponse> options, String defaultValue, String value, String tooltip) {
	}

	public record ValueResponse(String value, String label) {
	}

	public record ConceptResolveResponse(List<String> resolvedConcepts, List<String> unknownCodes) {
	}

	public static final class ConceptCodeList {
		public final @NotNull
		@NotEmpty List<@NotBlank String> concepts;

		@JsonCreator
		public ConceptCodeList(@JsonProperty("concepts") List<String> concepts) {
			this.concepts = concepts;
		}
	}

}
