package com.bakdata.conquery.sql.compiler;

/** Describes how a compiled output column contributes to a result row. */
public enum ColumnRole {

	/** Column containing an entity identifier. Multiple columns may form a composite identifier. */
	ENTITY_ID,

	/** Query result value decoded and post-processed by the backend. */
	RESULT
}
