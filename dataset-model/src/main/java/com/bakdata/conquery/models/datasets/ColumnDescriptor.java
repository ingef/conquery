package com.bakdata.conquery.models.datasets;

/** Framework-neutral description of a validated dataset column. */
public record ColumnDescriptor(String name, ColumnType type) {
}
