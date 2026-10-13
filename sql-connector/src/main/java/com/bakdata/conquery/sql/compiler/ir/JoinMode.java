package com.bakdata.conquery.sql.compiler.ir;

/** Supported ways to combine the tables produced by compiler query steps. */
public enum JoinMode {
	INNER,
	FULL_OUTER,
	LEFT
}
