package com.bakdata.conquery.sql.query;

import java.util.Objects;

/** Logical negation. */
public record NegationNode(QueryNode child) implements QueryNode {

	public NegationNode {
		Objects.requireNonNull(child, "child");
	}
}
