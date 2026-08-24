package com.bakdata.conquery.quarkus.plugin.api.filters;

/** Host services available while converting a plugin filter definition. */
public interface FilterConversionContext {

	/** Selects a stable local ID component and reports sanitized fallbacks through the host. */
	String idPartFromPreferredOrFallback(String preferred, String fallback, String idType, Object fallbackContext);

	/**
	 * Resolves and validates a local connector-table column.
	 *
	 * @throws IllegalArgumentException when the name is invalid or the column does not exist
	 */
	Column requireColumn(String columnName);

	/** Validated plugin-facing description of a table column. */
	record Column(String name, ColumnType type) {
	}

	/** Column types that plugins may use to select conversion behavior. */
	enum ColumnType {
		STRING,
		INTEGER,
		DECIMAL,
		REAL,
		MONEY,
		DATE,
		BOOLEAN
	}
}
