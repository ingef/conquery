package com.bakdata.conquery.sql.model.node;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.model.internal.ModelNormalization;

/** Logical conjunction with its already-derived validity-date behavior. */
public record AndNode(
		@NotEmpty List<@NotNull @Valid QueryNode> children,
		@NotNull DateAggregationAction dateAction,
		boolean createExists
) implements QueryNode {

	public AndNode {
		children = ModelNormalization.immutableCopy(children);
	}
}
