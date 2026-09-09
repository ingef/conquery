package com.bakdata.conquery.sql.compiler.ir;

/** Shared SQL aliases used by compiler transformations and their generated output columns. */
public enum SharedAliases {

	PRIMARY_COLUMN("primary_id"),
	SECONDARY_ID("secondary_id"),
	DATES_COLUMN("dates"),

	NOP_TABLE("nop_table"),

	INDEX_START("index_start"),
	QUARTER_START("quarter_start"),
	QUARTER_END("quarter_end"),
	YEAR_START("year_start"),
	YEAR_END("year_end"),
	YEAR_END_QUARTER_ALIGNED("year_end_quarter_aligned"),
	YEAR_ALIGNED_COUNT("year_aligned_count"),
	QUARTER_ALIGNED_COUNT("quarter_aligned_count"),
	DAY_ALIGNED_COUNT("day_aligned_count"),
	SERIES_INDEX("index"),
	RESOLUTION("resolution"),
	INDEX("index"),
	INDEX_SELECTOR("index_selector"),
	INDEX_START_POSITIVE("index_start_positive"),
	INDEX_START_NEGATIVE("index_start_negative"),
	STRATIFICATION_BOUNDS("stratification_bounds"),
	OBSERVATION_SCOPE("scope"),

	SOURCE("source");

	private final String alias;

	SharedAliases(String alias) {
		this.alias = alias;
	}

	public String getAlias() {
		return alias;
	}
}
