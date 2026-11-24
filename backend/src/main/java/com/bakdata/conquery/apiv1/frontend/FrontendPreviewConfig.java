package com.bakdata.conquery.apiv1.frontend;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.models.config.ColumnConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FrontendPreviewConfig {
	@Data
	public static class Labelled {
		private final String name;
		private final String label;
	}

	private final Collection<Labelled> all;
	@JsonProperty("default")
	private final Collection<Labelled> defaultConnectors;
	private final Collection<FilterId> searchFilters;
	/**
	 * Prioritized list of ID-names to use for resolving and displaying of the entityId. The priority is primarily relevant for user recognition.
	 * <p/>
	 * To identify the specific column-index {@link FullExecutionStatus#getColumnDescriptions()}, scan semantics {@link com.bakdata.conquery.models.types.SemanticType.IdT} with kind set to the name.
	 *
	 * @implSpec At least one column is always non-null. See {@link ColumnConfig#isPrimaryId()}.
	 */
	private final List<String> prioritizedIdColumns;
	/**
	 * Search concept needs to be parent of searchFilters, so frontend can resolve the filters.
	 */
	private final ConceptId searchConcept;
	private final ConnectorId searchConnector;
}
