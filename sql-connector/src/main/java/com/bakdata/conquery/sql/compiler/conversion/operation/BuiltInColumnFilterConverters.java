package com.bakdata.conquery.sql.compiler.conversion.operation;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.function.BiFunction;

import com.bakdata.conquery.sql.compiler.conversion.Converter;
import com.bakdata.conquery.sql.compiler.ir.SchemaSql;
import com.bakdata.conquery.sql.compiler.ir.condition.InclusiveRangeCondition;
import com.bakdata.conquery.sql.compiler.ir.condition.FlagCondition;
import com.bakdata.conquery.sql.compiler.ir.condition.StringValuesCondition;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereClauses;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereCondition;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.concept.SqlFilters;
import com.bakdata.conquery.sql.model.operation.BuiltInFilters;
import com.bakdata.conquery.sql.model.operation.ResolvedFilter;
import com.bakdata.conquery.sql.model.range.SubstringRange;
import org.jooq.Field;
import org.jooq.impl.DSL;

/** Built-in resolved filters that operate directly on connector columns. */
final class BuiltInColumnFilterConverters {

	private BuiltInColumnFilterConverters() {
	}

	static List<Converter<? extends ResolvedFilter, SqlFilters, FilterConversionContext>> create() {
		return List.of(
				converter(BuiltInFilters.StringValues.class, BuiltInColumnFilterConverters::convertStringValues),
				converter(BuiltInFilters.NumericColumnRange.class, BuiltInColumnFilterConverters::convertNumericRange),
				converter(BuiltInFilters.DateDistanceRange.class, BuiltInColumnFilterConverters::convertDateDistanceRange),
				converter(BuiltInFilters.Flags.class, BuiltInColumnFilterConverters::convertFlags)
		);
	}

	private static SqlFilters convertStringValues(
			BuiltInFilters.StringValues filter,
			FilterConversionContext context
	) {
		Field<String> field = SchemaSql.field(filter.column(), String.class);
		if (filter.substring().isPresent()) {
			field = substring(field, filter.substring().orElseThrow());
		}
		return eventFilter(new StringValuesCondition(field, filter.values().toArray(String[]::new)));
	}

	private static SqlFilters convertNumericRange(
			BuiltInFilters.NumericColumnRange filter,
			FilterConversionContext context
	) {
		Field<BigDecimal> field = SchemaSql.field(filter.column(), BigDecimal.class);
		return eventFilter(new InclusiveRangeCondition<>(field, filter.range()));
	}

	private static SqlFilters convertDateDistanceRange(
			BuiltInFilters.DateDistanceRange filter,
			FilterConversionContext context
	) {
		Field<Date> startDate = SchemaSql.field(filter.column(), Date.class);
		Field<BigDecimal> distance = context.dialect()
				.dateDistance(filter.unit(), startDate, filter.endDate())
				.coerce(BigDecimal.class);
		return eventFilter(new InclusiveRangeCondition<>(distance, filter.range()));
	}

	private static SqlFilters convertFlags(
			BuiltInFilters.Flags filter,
			FilterConversionContext context
	) {
		List<Field<Boolean>> selectedFlags = filter.selectedFlags().stream()
				.map(filter.availableFlags().columns()::get)
				.map(column -> SchemaSql.field(column, Boolean.class))
				.toList();
		return eventFilter(new FlagCondition(selectedFlags));
	}

	private static Field<String> substring(Field<String> field, SubstringRange range) {
		int start = range.startInclusive() + 1;
		return range.endExclusive()
				.map(end -> DSL.substring(field, start, end - range.startInclusive()))
				.orElseGet(() -> DSL.substring(field, start));
	}

	private static SqlFilters eventFilter(WhereCondition condition) {
		return new SqlFilters(
				ConnectorSqlSelects.none(),
				WhereClauses.builder().eventFilter(condition).build()
		);
	}

	private static <F extends ResolvedFilter> Converter<F, SqlFilters, FilterConversionContext> converter(
			Class<F> conversionClass,
			BiFunction<F, FilterConversionContext, SqlFilters> conversion
	) {
		return new Converter<>() {
			@Override
			public Class<F> getConversionClass() {
				return conversionClass;
			}

			@Override
			public SqlFilters convert(F input, FilterConversionContext context) {
				return conversion.apply(input, context);
			}
		};
	}
}
