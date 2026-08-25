package com.bakdata.conquery.quarkus.plugin.api.datasets;

/** Logical type of a dataset column. */
public enum ColumnType {
	STRING,
	INTEGER,
	BOOLEAN,
	REAL,
	DECIMAL,
	MONEY,
	DATE,
	DATE_RANGE
}
