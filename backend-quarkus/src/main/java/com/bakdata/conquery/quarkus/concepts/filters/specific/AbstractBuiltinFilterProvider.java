package com.bakdata.conquery.quarkus.concepts.filters.specific;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.concepts.filters.FilterDefinitionProvider;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.ids.FilterId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class AbstractBuiltinFilterProvider implements FilterDefinitionProvider<CommonFilterPayload> {

	@Override
	public Class<CommonFilterPayload> payloadType() {
		return CommonFilterPayload.class;
	}

	protected DatasetCatalogRepository.Filter filter(
			FilterConversionContext context,
			CommonFilterPayload payload,
			String frontendType,
			Integer min,
			Integer max,
			boolean creatable,
			List<ColumnId> requiredColumns
	) {
		String name = context.idPartFromPreferredOrFallback(payload.name(), payload.label(), "filter id", type());
		String label = firstNonBlank(payload.label(), payload.name()).orElse(name);
		FilterId id = context.filterId(name);
		return new DatasetCatalogRepository.Filter(
				id,
				label,
				frontendType,
				payload.unit(),
				payload.tooltip(),
				options(payload),
				min == null ? payload.min() : min,
				max == null ? payload.max() : max,
				payload.pattern(),
				payload.allowDropFile(),
				creatable,
				payload.defaultValue(),
				requiredColumns
		);
	}

	protected ColumnId requiredColumn(FilterConversionContext context, CommonFilterPayload payload) {
		String column = firstNonBlank(payload.column())
				.orElseThrow(() -> new IllegalArgumentException("Filter " + type() + " must define column."));
		return context.columnId(column);
	}

	protected List<ColumnId> optionalColumns(FilterConversionContext context, List<String> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream().map(context::columnId).toList();
	}

	protected List<DatasetCatalogRepository.FrontendValue> options(CommonFilterPayload payload) {
		if (payload.options() != null) {
			return payload.options().stream()
						  .map(value -> new DatasetCatalogRepository.FrontendValue(value.value(), value.label(), value.optionValue()))
						  .toList();
		}
		if (payload.labels() != null) {
			return payload.labels().entrySet().stream()
						  .map(entry -> new DatasetCatalogRepository.FrontendValue(entry.getKey(), entry.getValue(), entry.getKey()))
						  .toList();
		}
		return List.of();
	}

	protected List<ColumnId> columns(ColumnId primary, List<ColumnId> additional) {
		List<ColumnId> columns = new ArrayList<>();
		if (primary != null) {
			columns.add(primary);
		}
		if (additional != null) {
			columns.addAll(additional);
		}
		return columns;
	}

	protected List<ColumnId> flagColumns(FilterConversionContext context, Map<String, String> flags) {
		if (flags == null) {
			return List.of();
		}
		return flags.values().stream().map(context::columnId).toList();
	}

	protected String numericFrontendType(FilterConversionContext context, ColumnId column) {
		return switch (context.columnType(column)) {
			case MONEY -> "MONEY_RANGE";
			case INTEGER -> "INTEGER_RANGE";
			case DECIMAL, REAL -> "REAL_RANGE";
			default -> "REAL_RANGE";
		};
	}

	private java.util.Optional<String> firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return java.util.Optional.of(value.trim());
			}
		}
		return java.util.Optional.empty();
	}
}
