package com.bakdata.conquery.sql.compiler.query;

import java.util.Optional;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;

/** Query-step graph fragments produced for a resolved external node. */
public record ExternalQuerySteps(
		QueryStep entities,
		Optional<QueryStep> values
) {
}
