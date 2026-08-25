package com.bakdata.conquery.quarkus.plugin.api.filters;

import com.bakdata.conquery.quarkus.plugin.api.datasets.ColumnDescriptor;

/** Host services available while converting a plugin filter definition. */
public interface FilterConversionContext {

	/** Selects a stable local ID component and reports sanitized fallbacks through the host. */
	String idPartFromPreferredOrFallback(String preferred, String fallback, String idType, Object fallbackContext);

	/**
	 * Resolves and validates a local connector-table column.
	 *
	 * @throws IllegalArgumentException when the name is invalid or the column does not exist
	 */
	ColumnDescriptor requireColumn(String columnName);
}
