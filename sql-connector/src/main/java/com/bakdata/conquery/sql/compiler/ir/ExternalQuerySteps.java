package com.bakdata.conquery.sql.compiler.ir;

import java.util.Optional;

/** Query-step graph fragments produced for a resolved external node. */
public record ExternalQuerySteps(
		QueryStep entities,
		Optional<QueryStep> values
) {
}
