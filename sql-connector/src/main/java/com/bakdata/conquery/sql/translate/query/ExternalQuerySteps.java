package com.bakdata.conquery.sql.translate.query;

import java.util.Optional;

import com.bakdata.conquery.sql.translate.ir.QueryStep;

/** Query-step graph fragments produced for a resolved external node. */
public record ExternalQuerySteps(
		QueryStep entities,
		Optional<QueryStep> values
) {
}
