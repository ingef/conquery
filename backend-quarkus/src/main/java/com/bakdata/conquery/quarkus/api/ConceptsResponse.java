package com.bakdata.conquery.quarkus.api;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record ConceptsResponse(
		List<SecondaryIdResponse> secondaryIds,
		Map<String, ConceptSummaryResponse> concepts
) {
	public record SecondaryIdResponse(
			String id,
			String label,
			String description
	) {
	}

	public record ConceptSummaryResponse(
			String label,
			String description,
			@Schema(description = "Whether this node can be dragged/selected in the query editor. "
								  + "When detailsAvailable is false (folder/structure node), this should be false.")
			Boolean active,
			String parent,
			List<String> children,
			Long matchingEntries,
			Long matchingEntities,
			@Schema(description = "Whether detailed concept contents are available via /api/concepts/{conceptId}. "
								  + "If false, this node is a folder/structure node and should not be draggable.")
			Boolean detailsAvailable,
			Boolean codeListResolvable,
			List<ConceptResource.ConnectorResponse> tables,
			List<ConceptResource.SelectResponse> selects
	) {
	}
}
