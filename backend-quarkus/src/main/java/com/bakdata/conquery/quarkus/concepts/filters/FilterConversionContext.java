package com.bakdata.conquery.quarkus.concepts.filters;

import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.ids.ConnectorId;
import com.bakdata.conquery.quarkus.ids.FilterId;
import com.bakdata.conquery.quarkus.ids.IdPartSanitizer;
import com.bakdata.conquery.quarkus.ids.TableId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;

public record FilterConversionContext(
		ConnectorId connectorId,
		TableId tableId,
		DatasetCatalogRepository.TableRecord table,
		FallbackReporter fallbackReporter
) {

	public FilterConversionContext(ConnectorId connectorId, TableId tableId, DatasetCatalogRepository.TableRecord table) {
		this(connectorId, tableId, table, (idType, context, fallbackValue, sanitized) -> {
		});
	}

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

	public FilterId filterId(String name) {
		return new FilterId(connectorId, name);
	}

	public ColumnId columnId(String columnName) {
		if (columnName == null || columnName.isBlank()) {
			throw new IllegalArgumentException("Filter column in connector '" + connectorId + "' must not be blank.");
		}
		if (columnName.contains(".")) {
			throw new IllegalArgumentException("Filter column in connector '" + connectorId + "' must be a local column name without dots: " + columnName);
		}
		ColumnId columnId = new ColumnId(tableId, columnName);
		columnType(columnId);
		return columnId;
	}

	public DatasetCatalogRepository.ColumnType columnType(ColumnId columnId) {
		return table.columns().stream()
					.filter(column -> column.id().equals(columnId))
					.map(DatasetCatalogRepository.ColumnRecord::type)
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException("Unknown filter column '" + columnId + "' in table '" + tableId + "'."));
	}

	@FunctionalInterface
	public interface FallbackReporter {
		void record(String idType, Object context, String fallbackValue, String sanitized);
	}
}
