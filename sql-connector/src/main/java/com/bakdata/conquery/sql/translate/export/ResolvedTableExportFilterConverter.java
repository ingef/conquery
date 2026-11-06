package com.bakdata.conquery.sql.translate.export;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

import com.bakdata.conquery.sql.model.operation.BuiltInAggregations;
import com.bakdata.conquery.sql.model.operation.BuiltInFilters;
import com.bakdata.conquery.sql.model.operation.ResolvedAggregation;
import com.bakdata.conquery.sql.model.operation.ResolvedFilter;
import com.bakdata.conquery.sql.model.range.NumberRange;
import com.bakdata.conquery.sql.model.range.SubstringRange;
import com.bakdata.conquery.sql.model.schema.DateColumns;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.SchemaSql;
import com.bakdata.conquery.sql.translate.ir.condition.FlagCondition;
import com.bakdata.conquery.sql.translate.ir.condition.InclusiveRangeCondition;
import com.bakdata.conquery.sql.translate.ir.condition.StringValuesCondition;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

/** Converts resolved filters to predicates evaluated on individual table-export rows. */
public final class ResolvedTableExportFilterConverter {

	public Condition convert(ResolvedFilter filter, TranslatorDialect dialect) {
		return switch (filter) {
			case BuiltInFilters.StringValues values -> stringValues(values);
			case BuiltInFilters.NumericColumnRange range -> inclusive(
					SchemaSql.field(range.column(), BigDecimal.class), range.range());
			case BuiltInFilters.DateDistanceRange distance -> inclusive(
					dialect.dateDistance(distance.unit(), SchemaSql.field(distance.column(), Date.class), distance.endDate())
							.coerce(BigDecimal.class), distance.range());
			case BuiltInFilters.Flags flags -> flags(flags);
			case BuiltInFilters.AggregationRange aggregation -> inclusive(
					rowValue(aggregation.aggregation(), dialect).coerce(BigDecimal.class), aggregation.range());
			default -> throw new UnsupportedOperationException(
					"Table-export filter is not implemented for " + filter.getClass());
		};
	}

	private static Condition stringValues(BuiltInFilters.StringValues filter) {
		Field<String> field = SchemaSql.field(filter.column(), String.class);
		if (filter.substring().isPresent()) {
			field = substring(field, filter.substring().orElseThrow());
		}
		return new StringValuesCondition(field, filter.values().toArray(String[]::new)).condition();
	}

	private static Condition flags(BuiltInFilters.Flags filter) {
		List<Field<Boolean>> fields = filter.selectedFlags().stream()
				.map(filter.availableFlags().columns()::get)
				.map(column -> SchemaSql.field(column, Boolean.class))
				.toList();
		return new FlagCondition(fields).condition();
	}

	private static Field<? extends Number> rowValue(ResolvedAggregation aggregation, TranslatorDialect dialect) {
		return switch (aggregation) {
			case BuiltInAggregations.Count ignored -> DSL.inline(1);
			case BuiltInAggregations.CountQuarters ignored -> DSL.inline(1);
			case BuiltInAggregations.Sum sum -> sum.subtractColumn()
					.<Field<? extends Number>>map(subtract -> SchemaSql.field(sum.column(), BigDecimal.class)
							.minus(SchemaSql.field(subtract, BigDecimal.class)))
					.orElseGet(() -> SchemaSql.field(sum.column(), BigDecimal.class));
			case BuiltInAggregations.DurationSum duration -> duration(duration.dates(), dialect);
			case BuiltInAggregations.Flags ignored -> throw new IllegalArgumentException(
					"Flag filters use the dedicated resolved flag operation");
			default -> throw new UnsupportedOperationException(
					"Table-export aggregation is not implemented for " + aggregation.getClass());
		};
	}

	private static Field<Integer> duration(DateColumns dates, TranslatorDialect dialect) {
		return switch (dates) {
			case DateColumns.Single single -> {
				Field<Date> date = SchemaSql.field(single.column(), Date.class);
				yield dialect.dateDistance(java.time.temporal.ChronoUnit.DAYS, date, date);
			}
			case DateColumns.Pair pair -> dialect.dateDistance(java.time.temporal.ChronoUnit.DAYS,
					SchemaSql.field(pair.start(), Date.class), SchemaSql.field(pair.end(), Date.class));
		};
	}

	private static Condition inclusive(Field<BigDecimal> field, NumberRange range) {
		return new InclusiveRangeCondition<>(field, range).condition();
	}

	private static Field<String> substring(Field<String> field, SubstringRange range) {
		int start = range.startInclusive() + 1;
		return range.endExclusive()
				.map(end -> DSL.substring(field, start, end - range.startInclusive()))
				.orElseGet(() -> DSL.substring(field, start));
	}
}
