package com.bakdata.conquery.sql.model.node;

import com.bakdata.conquery.models.query.DateAggregationAction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Logical negation. */
public record NegationNode(
		@NotNull @Valid QueryNode child,
		@NotNull DateAggregationAction dateAction
) implements QueryNode {

	public NegationNode(QueryNode child) {
		this(child, DateAggregationAction.BLOCK);
	}
}
