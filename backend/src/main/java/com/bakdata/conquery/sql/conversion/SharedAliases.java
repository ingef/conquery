package com.bakdata.conquery.sql.conversion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SharedAliases {

	PRIMARY_COLUMN("primary_id"),
	SECONDARY_ID("secondary_id"),
	DATES_COLUMN("dates"),
	DATE_RESTRICTION("date_restriction"),

	NOP_TABLE("nop_table"),

	// form related (new approach)
	INDEX_START("index_start"),
	QUARTER_START("quarter_start"),
	YEAR_START("year_start"),
	YEAR_ALIGNED_COUNT("year_aligned_count"),
	QUARTER_ALIGNED_COUNT("quarter_aligned_count"),
	DAY_ALIGNED_COUNT("day_aligned_count"),
	SERIES_INDEX("index"),
	STRATIFICATION_BOUNDS("stratification_bounds"),

	// form related
	RESOLUTION("resolution"),
	INDEX("index"),
	STRATIFICATION_RANGE("stratification_range"),
	DATE_START("date_start"),
	DATE_END("date_end"),
	DATE_SERIES("date_series"),
	INDEX_DATE("index_date"),
	INDEX_START_POSITIVE("index_start_positive"),
	INDEX_START_NEGATIVE("index_start_negative");

	private final String alias;
}
