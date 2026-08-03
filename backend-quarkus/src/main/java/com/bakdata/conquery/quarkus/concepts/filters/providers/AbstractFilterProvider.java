package com.bakdata.conquery.quarkus.concepts.filters.providers;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.concepts.filters.FilterDefinitionProvider;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.AbstractFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.SelectFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.SingleColumnFilterDefinition;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.ids.FilterId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class AbstractFilterProvider<T extends AbstractFilterDefinition> implements FilterDefinitionProvider<T> {

	private final Class<T> payloadType;

	protected AbstractFilterProvider(Class<T> payloadType) {
		this.payloadType = payloadType;
	}

	@Override
	public Class<T> payloadType() {
		return payloadType;
	}

	protected DatasetCatalogRepository.Filter filter(
			FilterConversionContext context,
			T payload,
			String frontendType,
			Integer min,
			Integer max,
			boolean creatable,
			List<ColumnId> requiredColumns
	) {
		String name = context.idPartFromPreferredOrFallback(payload.getName(), payload.getLabel(), "filter id", type());
		String label = firstNonBlank(payload.getLabel(), payload.getName()).orElse(name);
		FilterId id = context.filterId(name);
		return new DatasetCatalogRepository.Filter(
				id,
				label,
				frontendType,
				payload.getUnit(),
				payload.getTooltip(),
				options(payload),
				min == null ? payload.getMin() : min,
				max == null ? payload.getMax() : max,
				payload.getPattern(),
				Boolean.TRUE.equals(payload.getAllowDropFile()),
				creatable,
				payload.getDefaultValue(),
				requiredColumns
		);
	}

	protected ColumnId requiredColumn(FilterConversionContext context, SingleColumnFilterDefinition payload) {
		String column = firstNonBlank(payload.getColumn())
				.orElseThrow(() -> new IllegalArgumentException("Filter " + type() + " must define column."));
		return context.columnId(column);
	}

	protected List<ColumnId> optionalColumns(FilterConversionContext context, List<String> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream().map(context::columnId).toList();
	}

	protected List<DatasetCatalogRepository.FrontendValue> options(AbstractFilterDefinition payload) {
		if (payload instanceof SelectFilterDefinition select && select.getOptions() != null) {
			return select.getOptions().stream()
						  .map(value -> new DatasetCatalogRepository.FrontendValue(value.value(), value.label(), value.optionValue()))
						  .toList();
		}
		if (payload instanceof SelectFilterDefinition select && select.getLabels() != null) {
			return select.getLabels().entrySet().stream()
						  .map(entry -> new DatasetCatalogRepository.FrontendValue(entry.getKey(), entry.getValue(), entry.getKey()))
						  .toList();
		}
		if (payload instanceof com.bakdata.conquery.quarkus.concepts.filters.definitions.FlagsFilterDefinition flags && flags.getFlags() != null) {
			return flags.getFlags().keySet().stream()
					.map(value -> new DatasetCatalogRepository.FrontendValue(value, value, value))
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
