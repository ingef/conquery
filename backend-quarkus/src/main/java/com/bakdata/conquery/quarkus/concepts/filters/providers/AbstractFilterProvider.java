package com.bakdata.conquery.quarkus.concepts.filters.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.quarkus.concepts.filters.definitions.FlagsFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.SelectFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.IntegerRangeFilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.MoneyRangeFilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.RealRangeFilterValue;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import com.bakdata.conquery.models.datasets.ColumnDescriptor;
import com.bakdata.conquery.quarkus.plugin.api.filters.AbstractFilterDefinition;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterDefinitionProvider;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterResult;
import com.bakdata.conquery.quarkus.plugin.api.filters.SingleColumnFilterDefinition;

abstract class AbstractFilterProvider<T extends AbstractFilterDefinition> implements FilterDefinitionProvider<T> {

	private final Class<T> modelType;
	private final Set<String> acceptedValueTypes;

	@SafeVarargs
	protected AbstractFilterProvider(Class<T> modelType, Class<? extends FilterValue>... acceptedValueTypes) {
		this.modelType = modelType;
		this.acceptedValueTypes = Set.of(acceptedValueTypes).stream()
				.map(AbstractFilterProvider::valueType)
				.collect(Collectors.toUnmodifiableSet());
	}

	@Override
	public Class<T> modelType() {
		return modelType;
	}

	@Override
	public Set<String> acceptedValueTypes() {
		return acceptedValueTypes;
	}

	protected FilterResult filter(
			FilterConversionContext context,
			T payload,
			Class<? extends FilterValue> valueType,
			Integer min,
			Integer max,
			boolean creatable,
			List<ColumnDescriptor> requiredColumns
	) {
		String name = context.idPartFromPreferredOrFallback(payload.getName(), payload.getLabel(), "filter id", type());
		String label = firstNonBlank(payload.getLabel(), payload.getName()).orElse(name);
		return new FilterResult(
				name,
				label,
				frontendType(valueType),
				payload.getUnit(),
				payload.getTooltip(),
				options(payload),
				min == null ? payload.getMin() : min,
				max == null ? payload.getMax() : max,
				payload.getPattern(),
				Boolean.TRUE.equals(payload.getAllowDropFile()),
				creatable,
				payload.getDefaultValue(),
				requiredColumns.stream().map(ColumnDescriptor::name).toList()
		);
	}

	protected String frontendType(Class<? extends FilterValue> valueType) {
		String type = valueType(valueType);
		if (!acceptedValueTypes.contains(type)) {
			throw new IllegalArgumentException("Filter provider " + getClass().getName()
					+ " does not accept filter value type " + type);
		}
		return type;
	}

	protected ColumnDescriptor requiredColumn(FilterConversionContext context, SingleColumnFilterDefinition payload) {
		String column = firstNonBlank(payload.getColumn())
				.orElseThrow(() -> new IllegalArgumentException("Filter " + type() + " must define column."));
		return context.requireColumn(column);
	}

	protected List<ColumnDescriptor> optionalColumns(FilterConversionContext context, List<String> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream().map(context::requireColumn).toList();
	}

	protected List<FilterResult.Option> options(AbstractFilterDefinition payload) {
		if (payload instanceof SelectFilterDefinition select && select.getOptions() != null) {
			return select.getOptions().stream()
					.map(value -> new FilterResult.Option(value.value(), value.label(), value.optionValue()))
					.toList();
		}
		if (payload instanceof SelectFilterDefinition select && select.getLabels() != null) {
			return select.getLabels().entrySet().stream()
					.map(entry -> new FilterResult.Option(entry.getKey(), entry.getValue(), entry.getKey()))
					.toList();
		}
		if (payload instanceof FlagsFilterDefinition flags && flags.getFlags() != null) {
			return flags.getFlags().keySet().stream()
					.map(value -> new FilterResult.Option(value, value, value))
					.toList();
		}
		return List.of();
	}

	protected List<ColumnDescriptor> columns(ColumnDescriptor primary, List<ColumnDescriptor> additional) {
		List<ColumnDescriptor> columns = new ArrayList<>();
		if (primary != null) {
			columns.add(primary);
		}
		if (additional != null) {
			columns.addAll(additional);
		}
		return columns;
	}

	protected List<ColumnDescriptor> flagColumns(FilterConversionContext context, Map<String, String> flags) {
		if (flags == null) {
			return List.of();
		}
		return flags.values().stream().map(context::requireColumn).toList();
	}

	protected Class<? extends FilterValue> numericRangeValueType(ColumnDescriptor column) {
		return switch (column.type()) {
			case MONEY -> MoneyRangeFilterValue.class;
			case INTEGER -> IntegerRangeFilterValue.class;
			case DECIMAL, REAL -> RealRangeFilterValue.class;
			default -> RealRangeFilterValue.class;
		};
	}

	private static String valueType(Class<? extends FilterValue> valueType) {
		PolymorphicModelSubtype subtype = valueType.getAnnotation(PolymorphicModelSubtype.class);
		if (subtype == null) {
			throw new IllegalArgumentException("Filter value " + valueType.getName() + " has no @PolymorphicModelSubtype");
		}
		return subtype.id();
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
