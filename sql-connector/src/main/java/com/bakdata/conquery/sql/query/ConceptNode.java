package com.bakdata.conquery.sql.query;

import java.util.List;
import java.util.Objects;

/** A concept selection whose connectors, conditions, filters, and selects are fully resolved. */
public record ConceptNode(
		String logicalId,
		List<ResolvedConnector> connectors,
		List<ResolvedSelect> selects,
		DateAggregationAction dateAction
) implements QueryNode {

	public ConceptNode {
		logicalId = ModelValidation.requireNonBlank(logicalId, "logicalId");
		ModelValidation.requireNotEmpty(connectors, "connectors");
		connectors = List.copyOf(connectors);
		selects = List.copyOf(Objects.requireNonNull(selects, "selects"));
		dateAction = Objects.requireNonNull(dateAction, "dateAction");
	}
}
