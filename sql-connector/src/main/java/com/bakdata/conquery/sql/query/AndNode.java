package com.bakdata.conquery.sql.query;

import java.util.List;
import java.util.Objects;

/** Logical conjunction with its already-derived validity-date behavior. */
public record AndNode(List<QueryNode> children, DateAggregationAction dateAction, boolean createExists) implements QueryNode {

	public AndNode {
		ModelValidation.requireNotEmpty(children, "children");
		children = List.copyOf(children);
		dateAction = Objects.requireNonNull(dateAction, "dateAction");
	}
}
