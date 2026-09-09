package com.bakdata.conquery.sql.model.node;

import java.util.List;

import com.bakdata.conquery.sql.model.internal.ModelNormalization;
import com.bakdata.conquery.sql.model.operation.ResolvedSelect;
import com.bakdata.conquery.sql.model.schema.ResolvedConnector;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** A concept selection whose connectors, conditions, filters, and selects are fully resolved. */
public record ConceptNode(
		@NotBlank String logicalId,
		@NotEmpty List<@NotNull @Valid ResolvedConnector> connectors,
		@NotNull List<@NotNull @Valid ResolvedSelect> selects,
		@NotNull DateAggregationAction dateAction
) implements QueryNode {

	public ConceptNode {
		connectors = ModelNormalization.immutableCopy(connectors);
		selects = ModelNormalization.immutableCopy(selects);
	}
}
