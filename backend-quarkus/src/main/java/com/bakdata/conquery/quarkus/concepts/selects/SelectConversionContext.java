package com.bakdata.conquery.quarkus.concepts.selects;

import java.util.Optional;

import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.ids.ConnectorId;
import com.bakdata.conquery.quarkus.ids.IdPartSanitizer;
import com.bakdata.conquery.quarkus.ids.SelectId;
import com.bakdata.conquery.quarkus.ids.TableId;
import com.bakdata.conquery.quarkus.plugin.api.datasets.ColumnType;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;

public record SelectConversionContext(
		ConnectorId connectorId,
		TableId tableId,
		DatasetCatalogRepository.TableRecord table,
		FallbackReporter fallbackReporter
) {

	public SelectId selectId(String name) {
		return new SelectId(connectorId, name);
	}

	public ColumnId columnId(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Select column in connector '" + connectorId + "' must not be blank.");
		}
		if (value.contains(".")) {
			throw new IllegalArgumentException("Select column in connector '" + connectorId + "' must be a local column name without dots: " + value);
		}
		ColumnId columnId = new ColumnId(tableId, value);
		if (table.columns().stream().noneMatch(column -> column.id().equals(columnId))) {
			throw new IllegalArgumentException("Select for connector '" + connectorId + "' references unknown column '" + columnId + "'.");
		}
		return columnId;
	}

	public ColumnType columnType(ColumnId columnId) {
		return table.columns().stream()
				.filter(column -> column.id().equals(columnId))
				.map(DatasetCatalogRepository.ColumnRecord::type)
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown column '" + columnId + "'."));
	}

	public String idPartFromPreferredOrFallback(String preferred, String fallback, String label, String type) {
		if (preferred != null && !preferred.isBlank()) {
			return preferred.trim();
		}
		String source = Optional.ofNullable(fallback).filter(value -> !value.isBlank())
				.orElseThrow(() -> new IllegalArgumentException("Select " + type + " must define name or label."));
		String sanitized = IdPartSanitizer.sanitize(source, label);
		fallbackReporter.record(label, type, source, sanitized);
		return sanitized;
	}

	@FunctionalInterface
	public interface FallbackReporter {
		void record(String idType, Object context, String fallbackValue, String sanitized);
	}
}
