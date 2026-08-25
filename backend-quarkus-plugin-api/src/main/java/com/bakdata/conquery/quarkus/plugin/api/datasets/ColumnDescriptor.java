package com.bakdata.conquery.quarkus.plugin.api.datasets;

/** Stable plugin-facing description of a validated dataset column. */
public record ColumnDescriptor(String name, ColumnType type) {
}
