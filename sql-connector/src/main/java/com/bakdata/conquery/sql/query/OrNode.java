package com.bakdata.conquery.sql.query;

import java.util.List;
import java.util.Objects;

/** Logical disjunction with its already-derived validity-date behavior. */
public record OrNode(List<QueryNode> children, DateAggregationAction dateAction,
                     boolean createExists) implements QueryNode {

	public OrNode {
		ModelValidation.requireNotEmpty(children, "children");
		children = List.copyOf(children);
		Objects.requireNonNull(dateAction, "dateAction");
	}
}
