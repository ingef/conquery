package com.bakdata.conquery.sql.conversion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.concepts.conditions.AndCondition;
import com.bakdata.conquery.models.datasets.concepts.conditions.ColumnEqualCondition;
import com.bakdata.conquery.models.datasets.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.datasets.concepts.conditions.EqualCondition;
import com.bakdata.conquery.models.datasets.concepts.conditions.IsEmptyCondition;
import com.bakdata.conquery.models.datasets.concepts.conditions.IsPresentCondition;
import com.bakdata.conquery.models.datasets.concepts.conditions.NotCondition;
import com.bakdata.conquery.models.datasets.concepts.conditions.PrefixCondition;
import com.bakdata.conquery.models.datasets.concepts.conditions.PrefixRangeCondition;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.CountFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.CountQuartersFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.DateDistanceFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.DurationSumFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.FlagFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.NumberFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.concept.ConceptColumnSelect;
import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.EventDateUnionSelect;
import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.EventDurationSumSelect;
import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.ExistsSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.DistinctSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.FirstValueSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.LastValueSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.RandomValueSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.CountQuartersSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.CountSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DateDistanceSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DateUnionSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DurationSumSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.FlagSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.MappableSingleColumnSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.SumSelect;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.sql.conversion.model.EntitySchemaAdapter;
import com.bakdata.conquery.sql.model.operation.BuiltInAggregations;
import com.bakdata.conquery.sql.model.operation.BuiltInConditions;
import com.bakdata.conquery.sql.model.operation.BuiltInFilters;
import com.bakdata.conquery.sql.model.operation.BuiltInSelects;
import com.bakdata.conquery.sql.model.operation.ResolvedAggregation;
import com.bakdata.conquery.sql.model.operation.ResolvedCondition;
import com.bakdata.conquery.sql.model.operation.ResolvedFilter;
import com.bakdata.conquery.sql.model.operation.ResolvedSelect;
import com.bakdata.conquery.sql.model.range.NumberRange;
import com.bakdata.conquery.sql.model.range.SubstringRange;
import com.bakdata.conquery.sql.model.result.ResultColumn;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import lombok.experimental.UtilityClass;

/** Converts already-resolved backend operations into the connector's immutable input model. */
@UtilityClass
public class ResolvedOperationAdapter {

	public static ResolvedSelect select(Select select, List<ResolvedColumn> conceptColumns, LocalDate endDate) {
		return switch (select) {
			case ConceptColumnSelect value -> new BuiltInSelects.ConceptValues(value.getName(), conceptColumns);
			case EventDateUnionSelect value -> new BuiltInSelects.EventDateUnion(value.getName());
			case EventDurationSumSelect value -> new BuiltInSelects.EventDurationSum(value.getName());
			case ExistsSelect value -> new BuiltInSelects.Exists(value.getName());
			case DistinctSelect value -> values(value, BuiltInSelects.ValueOperation.DISTINCT);
			case FirstValueSelect value -> values(value, BuiltInSelects.ValueOperation.FIRST);
			case LastValueSelect value -> values(value, BuiltInSelects.ValueOperation.LAST);
			case RandomValueSelect value -> values(value, BuiltInSelects.ValueOperation.RANDOM);
			case CountSelect value -> aggregation(value.getName(), count(value.getColumn().resolve(), value.isDistinct()));
			case CountQuartersSelect value -> aggregation(value.getName(), new BuiltInAggregations.CountQuarters(EntitySchemaAdapter.from(value)));
			case DurationSumSelect value -> aggregation(value.getName(), new BuiltInAggregations.DurationSum(
					EntitySchemaAdapter.from(value), List.of()));
			case SumSelect value -> aggregation(value.getName(), sum(
					value.getColumn().resolve(), value.getSubtractColumn(), value.getDistinctByColumn()));
			case FlagSelect value -> aggregation(value.getName(), flags(value.getFlags()));
			case DateDistanceSelect value -> new BuiltInSelects.DateDistance(
					value.getName(), EntitySchemaAdapter.from(value.getColumn().resolve()), value.getTimeUnit(), endDate);
			case DateUnionSelect value -> new BuiltInSelects.DateUnion(value.getName(), EntitySchemaAdapter.from(value));
			default -> throw new UnsupportedOperationException("SQL resolved select is not implemented for " + select.getClass());
		};
	}

	public static ResolvedFilter filter(FilterValue<?> filterValue, LocalDate endDate) {
		Filter<?> filter = filterValue.getFilter().resolve();
		Object value = filterValue.readValue();
		return switch (filter) {
			case SelectFilter<?> selectFilter -> new BuiltInFilters.StringValues(
					filter.getName(), EntitySchemaAdapter.from(selectFilter.getColumn().resolve()), stringValues(value),
					substring(selectFilter.getSubstringRange()));
			case NumberFilter<?> numberFilter -> new BuiltInFilters.NumericColumnRange(
					filter.getName(), EntitySchemaAdapter.from(numberFilter.getColumn().resolve()), numberRange((IRange<?, ?>) value));
			case CountFilter countFilter -> aggregationFilter(filter.getName(),
					count(countFilter.getColumn().resolve(), countFilter.isDistinct()), value);
			case CountQuartersFilter quartersFilter -> aggregationFilter(filter.getName(),
					new BuiltInAggregations.CountQuarters(EntitySchemaAdapter.from(quartersFilter)), value);
			case DurationSumFilter durationFilter -> aggregationFilter(filter.getName(),
					new BuiltInAggregations.DurationSum(EntitySchemaAdapter.from(durationFilter), List.of()), value);
			case SumFilter<?> sumFilter -> aggregationFilter(filter.getName(),
					sum(sumFilter.getColumn().resolve(), sumFilter.getSubtractColumn(), sumFilter.getDistinctByColumn()), value);
			case FlagFilter flagFilter -> new BuiltInFilters.Flags(filter.getName(), flags(flagFilter.getFlags()), (Set<String>) value);
			case DateDistanceFilter distanceFilter -> new BuiltInFilters.DateDistanceRange(
					filter.getName(), EntitySchemaAdapter.from(distanceFilter.getColumn().resolve()), distanceFilter.getTimeUnit(),
					endDate, numberRange((IRange<?, ?>) value));
			default -> throw new UnsupportedOperationException("SQL resolved filter is not implemented for " + filter.getClass());
		};
	}

	public static ResolvedCondition condition(CTCondition condition, SqlTable table, Optional<ResolvedColumn> connectorColumn) {
		return switch (condition) {
			case AndCondition value -> new BuiltInConditions.AllOf(value.getConditions().stream()
					.map(child -> condition(child, table, connectorColumn)).toList());
			case NotCondition value -> new BuiltInConditions.Not(condition(value.getCondition(), table, connectorColumn));
			case EqualCondition value -> new BuiltInConditions.StringValues(connectorColumn.orElseThrow(), value.getValues());
			case ColumnEqualCondition value -> new BuiltInConditions.StringValues(stringColumn(table, value.getColumn()), value.getValues());
			case IsPresentCondition value -> new BuiltInConditions.Presence(stringColumn(table, value.getColumn()), true);
			case IsEmptyCondition value -> new BuiltInConditions.Presence(stringColumn(table, value.getColumn()), false);
			case PrefixCondition value -> new BuiltInConditions.Prefixes(connectorColumn.orElseThrow(), List.of(value.getPrefixes()));
			case PrefixRangeCondition value -> new BuiltInConditions.PrefixRange(
					connectorColumn.orElseThrow(), value.getMin(), value.getMax());
			default -> throw new UnsupportedOperationException("SQL resolved condition is not implemented for " + condition.getClass());
		};
	}

	public static List<ResultColumn> resultColumns(List<ResultInfo> infos) {
		return java.util.stream.IntStream.range(0, infos.size())
				.mapToObj(index -> new ResultColumn("result-%d".formatted(index), resultType(infos.get(index).getType())))
				.toList();
	}

	private static BuiltInSelects.Aggregation aggregation(String name, ResolvedAggregation aggregation) {
		return new BuiltInSelects.Aggregation(name, aggregation);
	}

	private static BuiltInFilters.AggregationRange aggregationFilter(String name, ResolvedAggregation aggregation, Object range) {
		return new BuiltInFilters.AggregationRange(name, aggregation, numberRange((IRange<?, ?>) range));
	}

	private static BuiltInSelects.Values values(MappableSingleColumnSelect select, BuiltInSelects.ValueOperation operation) {
		return new BuiltInSelects.Values(select.getName(), EntitySchemaAdapter.from(select.getColumn().resolve()), operation,
				substring(select.getSubstringRange()));
	}

	private static Optional<SubstringRange> substring(Range.IntegerRange range) {
		if (range == null || range.isAll()) {
			return Optional.empty();
		}
		return Optional.of(new SubstringRange(range.getMin() == null ? 0 : range.getMin(), Optional.ofNullable(range.getMax())));
	}

	private static BuiltInAggregations.Count count(com.bakdata.conquery.models.datasets.Column column, boolean distinct) {
		ResolvedColumn resolved = EntitySchemaAdapter.from(column);
		return new BuiltInAggregations.Count(resolved, distinct ? List.of(resolved) : List.of());
	}

	private static BuiltInAggregations.Sum sum(
			com.bakdata.conquery.models.datasets.Column column,
			com.bakdata.conquery.models.identifiable.ids.specific.ColumnId subtract,
			List<com.bakdata.conquery.models.identifiable.ids.specific.ColumnId> distinctBy
	) {
		return new BuiltInAggregations.Sum(EntitySchemaAdapter.from(column),
				Optional.ofNullable(subtract).map(id -> EntitySchemaAdapter.from(id.resolve())), columns(distinctBy));
	}

	private static BuiltInAggregations.Flags flags(Map<String, com.bakdata.conquery.models.identifiable.ids.specific.ColumnId> flags) {
		Map<String, ResolvedColumn> resolved = new LinkedHashMap<>();
		flags.forEach((name, column) -> resolved.put(name, EntitySchemaAdapter.from(column.resolve())));
		return new BuiltInAggregations.Flags(resolved);
	}

	private static List<ResolvedColumn> columns(List<com.bakdata.conquery.models.identifiable.ids.specific.ColumnId> columns) {
		return Optional.ofNullable(columns).orElseGet(List::of).stream()
				.map(id -> EntitySchemaAdapter.from(id.resolve())).toList();
	}

	private static Set<String> stringValues(Object value) {
		return value instanceof Set<?> values
				? values.stream().map(String.class::cast).collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new))
				: Set.of((String) value);
	}

	private static NumberRange numberRange(IRange<?, ?> range) {
		return new NumberRange(
				Optional.ofNullable((Number) range.getMin()).map(ResolvedOperationAdapter::decimal),
				Optional.ofNullable((Number) range.getMax()).map(ResolvedOperationAdapter::decimal));
	}

	private static BigDecimal decimal(Number value) {
		return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
	}

	private static ResolvedColumn stringColumn(SqlTable table, String physicalName) {
		return new ResolvedColumn(table.logicalId() + "." + physicalName, table, physicalName,
				com.bakdata.conquery.models.datasets.ColumnType.STRING, true);
	}

	private static com.bakdata.conquery.sql.model.result.ResultType resultType(ResultType type) {
		if (type instanceof ResultType.ListT<?> list) {
			return new com.bakdata.conquery.sql.model.result.ResultType.ListType(primitive((ResultType.Primitive) list.getElementType()));
		}
		return primitive((ResultType.Primitive) type);
	}

	private static com.bakdata.conquery.sql.model.result.ResultType.Primitive primitive(ResultType.Primitive type) {
		return com.bakdata.conquery.sql.model.result.ResultType.Primitive.valueOf(type.name());
	}
}
