package com.bakdata.conquery.sql.model.node;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.sql.model.range.DateRange;

/** Applies an inclusive date restriction to its child. */
public record DateRestrictionNode(
		@NotNull @Valid DateRange dateRange,
		@NotNull @Valid QueryNode child
) implements QueryNode {
}
