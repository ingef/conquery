package com.bakdata.conquery.sql.query.node;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Logical negation. */
public record NegationNode(@NotNull @Valid QueryNode child) implements QueryNode {
}
