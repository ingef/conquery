package com.bakdata.conquery.sql.conversion.model;

/**
 * A subset of {@link org.jooq.JoinType} for join types that Conquery supports.
 */
public enum ConqueryJoinType {
	INNER_JOIN,
	OUTER_JOIN,
	LEFT_JOIN
}
