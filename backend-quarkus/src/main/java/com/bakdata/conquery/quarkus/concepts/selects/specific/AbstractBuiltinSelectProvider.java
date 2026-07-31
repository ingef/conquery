package com.bakdata.conquery.quarkus.concepts.selects.specific;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.quarkus.concepts.selects.SelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.SelectDefinitionProvider;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.AbstractSelectDefinition;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.DateRangeSelectDefinition;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.ids.SelectId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;

abstract class AbstractBuiltinSelectProvider<T extends AbstractSelectDefinition> implements SelectDefinitionProvider<T> {

	private final Class<T> payloadType;

	protected AbstractBuiltinSelectProvider(Class<T> payloadType) {
		this.payloadType = payloadType;
	}

	@Override
	public Class<T> payloadType() {
		return payloadType;
	}

	protected DatasetCatalogRepository.Select select(
			SelectConversionContext context,
			T payload,
			DatasetCatalogRepository.SelectResultType resultType,
			List<ColumnId> requiredColumns
	) {
		String name = context.idPartFromPreferredOrFallback(payload.getName(), payload.getLabel(), "select id", type());
		String label = firstNonBlank(payload.getLabel(), payload.getName()).orElse(name);
		SelectId id = context.selectId(name);
		return new DatasetCatalogRepository.Select(id, label, payload.getDescription(), payload.isDefault(), type(), resultType, requiredColumns);
	}

	protected List<ColumnId> optionalColumns(SelectConversionContext context, List<String> columns) {
		if (columns == null) {
			return List.of();
		}
		return columns.stream().map(context::columnId).toList();
	}

	protected List<ColumnId> dateRangeColumns(SelectConversionContext context, DateRangeSelectDefinition payload) {
		boolean hasColumn = isPresent(payload.getColumn());
		boolean hasStart = isPresent(payload.getStartColumn());
		boolean hasEnd = isPresent(payload.getEndColumn());
		if (hasColumn == (hasStart || hasEnd) || hasStart != hasEnd) {
			throw new IllegalArgumentException("Select " + type() + " must define either column or both startColumn and endColumn.");
		}
		if (hasColumn) {
			return List.of(context.columnId(payload.getColumn()));
		}
		return List.of(context.columnId(payload.getStartColumn()), context.columnId(payload.getEndColumn()));
	}

	protected List<ColumnId> append(List<ColumnId> first, List<ColumnId> second) {
		List<ColumnId> columns = new ArrayList<>(first);
		columns.addAll(second);
		return List.copyOf(columns);
	}

	protected DatasetCatalogRepository.SelectResultType resultType(SelectConversionContext context, ColumnId column) {
		return DatasetCatalogRepository.SelectResultType.primitive(context.columnType(column).name());
	}

	protected void requireColumnType(SelectConversionContext context, ColumnId column, DatasetCatalogRepository.ColumnType... accepted) {
		DatasetCatalogRepository.ColumnType actual = context.columnType(column);
		if (!Set.of(accepted).contains(actual)) {
			throw new IllegalArgumentException("Select " + type() + " column '" + column + "' has type " + actual + ", expected one of " + Set.of(accepted) + ".");
		}
	}

	protected void requireDateRangeTypes(SelectConversionContext context, List<ColumnId> columns) {
		if (columns.size() == 1) {
			requireColumnType(context, columns.getFirst(), DatasetCatalogRepository.ColumnType.DATE, DatasetCatalogRepository.ColumnType.DATE_RANGE);
			return;
		}
		columns.forEach(column -> requireColumnType(context, column, DatasetCatalogRepository.ColumnType.DATE));
	}

	protected DatasetCatalogRepository.SelectResultType primitive(String type) {
		return DatasetCatalogRepository.SelectResultType.primitive(type);
	}

	protected DatasetCatalogRepository.SelectResultType list(String elementType) {
		return DatasetCatalogRepository.SelectResultType.list(primitive(elementType));
	}

	private boolean isPresent(String value) {
		return value != null && !value.isBlank();
	}

	private Optional<String> firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return Optional.of(value.trim());
			}
		}
		return Optional.empty();
	}
}
