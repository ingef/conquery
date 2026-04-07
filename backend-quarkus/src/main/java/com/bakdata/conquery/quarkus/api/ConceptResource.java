package com.bakdata.conquery.quarkus.api;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.quarkus.api.config.ConceptsRuntimeConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;

@Path("/api/concepts")
@Produces(MediaType.APPLICATION_JSON)
public class ConceptResource {

	@Inject
	ConceptsRuntimeConfig conceptsConfig;

	@GET
	@Path("/{conceptId}")
	@Operation(
			summary = "Get concept details",
			description = "Returns a concept map keyed by concept id, compatible with frontend tree loading."
	)
	public Map<String, ConceptNodeResponse> getConcept(@PathParam("conceptId") String conceptId) {
		ConceptsRuntimeConfig.ConceptEntry concept = conceptsConfig.concepts()
																 .stream()
																 .filter(entry -> entry.id().equals(conceptId))
																 .findFirst()
																 .orElse(null);

		ConceptNodeResponse node = new ConceptNodeResponse(
				concept != null ? concept.label() : conceptId,
				null,
				true,
				List.of(),
				0L,
				0L,
				true,
				false,
				List.of(),
				List.of()
		);

		return Map.of(conceptId, node);
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
			List<String> supportedSecondaryIds,
			DateColumnResponse dateColumn
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
}
