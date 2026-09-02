package com.bakdata.conquery.sql.compiler.ir;

/** Determines how a query step's projection is represented during SQL rendering. */
public enum ProjectionMode {
	/** Keep the separate compiler fields required by subsequent query steps. */
	INTERMEDIATE,
	/** Render a final projection and aggregate validity ranges across grouped rows. */
	AGGREGATED,
	/** Render a final projection while preserving the validity range of each row. */
	INDIVIDUAL
}
