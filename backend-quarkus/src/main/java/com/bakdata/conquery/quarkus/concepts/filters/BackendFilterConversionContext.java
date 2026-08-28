package com.bakdata.conquery.quarkus.concepts.filters;

import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.ids.ConnectorId;
import com.bakdata.conquery.quarkus.ids.IdPartSanitizer;
import com.bakdata.conquery.quarkus.ids.TableId;
import com.bakdata.conquery.models.datasets.ColumnDescriptor;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;

record BackendFilterConversionContext(
		ConnectorId connectorId,
		TableId tableId,
		DatasetCatalogRepository.TableRecord table,
		FilterFallbackReporter fallbackReporter
) implements FilterConversionContext {

	@Override
	public String idPartFromPreferredOrFallback(String preferred, String fallback, String idType, Object fallbackContext) {
		if (preferred != null && !preferred.isBlank()) {
			return preferred.trim();
		}
		if (fallback == null || fallback.isBlank()) {
			throw new IllegalArgumentException("Cannot derive " + idType + " for " + fallbackContext + " because preferred and fallback values are blank.");
		}
		String fallbackValue = fallback.trim();
		String sanitized = IdPartSanitizer.sanitize(fallbackValue, idType + " fallback");
		fallbackReporter.record(idType, fallbackContext, fallbackValue, sanitized);
		return sanitized;
	}

	@Override
	public ColumnDescriptor requireColumn(String columnName) {
		ColumnId columnId = columnId(columnName);
		ColumnDescriptor resolvedColumn = table.columns().stream()
				.filter(column -> column.id().equals(columnId))
				.map(record -> new ColumnDescriptor(columnName, record.type()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown filter column '" + columnId + "' in table '" + tableId + "'."));
		return resolvedColumn;
	}

	ColumnId columnId(String columnName) {
		if (columnName == null || columnName.isBlank()) {
			throw new IllegalArgumentException("Filter column in connector '" + connectorId + "' must not be blank.");
		}
		if (columnName.contains(".")) {
			throw new IllegalArgumentException("Filter column in connector '" + connectorId + "' must be a local column name without dots: " + columnName);
		}
		return new ColumnId(tableId, columnName);
	}
}
