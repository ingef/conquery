package com.bakdata.conquery.sql.translate.ir;

/** Supported ways to combine the tables produced by translate query steps. */
public enum JoinMode {
	INNER,
	FULL_OUTER,
	LEFT
}
