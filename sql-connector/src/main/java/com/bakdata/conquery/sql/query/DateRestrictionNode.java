package com.bakdata.conquery.sql.query;

import java.util.Objects;

/** Applies an inclusive date restriction to its child. */
public record DateRestrictionNode(DateRange dateRange, QueryNode child) implements QueryNode {

	public DateRestrictionNode {
		dateRange = Objects.requireNonNull(dateRange, "dateRange");
		child = Objects.requireNonNull(child, "child");
	}
}
