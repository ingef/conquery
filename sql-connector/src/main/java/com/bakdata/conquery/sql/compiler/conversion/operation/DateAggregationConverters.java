package com.bakdata.conquery.sql.compiler.conversion.operation;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.concept.CommonAggregationSelect;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.interval.IntervalPackingSelectCompiler;
import com.bakdata.conquery.sql.compiler.ir.interval.IntervalPackingSelectPreparation;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.ir.select.SingleColumnSqlSelect;
import com.bakdata.conquery.sql.model.operation.BuiltInAggregations;
import com.bakdata.conquery.sql.model.schema.DateColumns;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import lombok.experimental.UtilityClass;
import org.jooq.Field;
import org.jooq.impl.DSL;

/** Date aggregations retain the legacy quarter-count and interval-packing semantics. */
@UtilityClass
final class DateAggregationConverters {

	static CommonAggregationSelect<?> countQuarters(BuiltInAggregations.CountQuarters aggregation, AggregationConversionContext context) {
		CompilerDialect dialect = context.dialect();
		String source = context.tables().getPredecessor(ConceptCteStep.AGGREGATION_SELECT);
		if (aggregation.dates() instanceof DateColumns.Single single && single.column().type() == ColumnType.DATE) {
			ExtractingSqlSelect<Date> root = new ExtractingSqlSelect<>(context.tables().getRootTable(), single.column().physicalName(), Date.class);
			Field<Integer> count = DSL.nullif(DSL.countDistinct(dialect.yearQuarter(root.qualify(source).select())), 0);
			return CommonAggregationSelect.<Integer>builder()
					.rootSelect(root)
					.groupBy(new FieldWrapper<>(count.as(context.alias()), single.column().physicalName()))
					.build();
		}

		ColumnDateRange inclusiveDates = inclusiveDates(aggregation.dates(), context);
		Field<Date> firstQuarter = dialect.quarterStart(inclusiveDates.getStart());
		Field<Date> afterLastQuarter = dialect.nextQuarterStart(inclusiveDates.getEnd());
		Field<Integer> quarters = dialect.dateDistance(ChronoUnit.MONTHS, firstQuarter, afterLastQuarter).divide(3);
		FieldWrapper<Integer> root = new FieldWrapper<>(quarters.as(context.alias()), requiredColumns(aggregation.dates()));
		// Deliberately preserve the legacy sum of quarters per event, including overlapping events.
		Field<BigDecimal> count = DSL.nullif(DSL.sum(root.qualify(source).select()), BigDecimal.ZERO);
		return CommonAggregationSelect.<BigDecimal>builder()
				.rootSelect(root)
				.groupBy(new FieldWrapper<>(count.as(context.alias()), context.alias()))
				.build();
	}

	static CommonAggregationSelect<BigDecimal> durationSum(BuiltInAggregations.DurationSum aggregation, AggregationConversionContext context) {
		// Preserve the existing SQL path: intervals are packed without applying distinctBy.
		CompilerDialect dialect = context.dialect();
		ColumnDateRange dates = dateRange(aggregation.dates(), context).as(context.alias());
		List<SingleColumnSqlSelect> roots = dates.toFields().stream()
				.<SingleColumnSqlSelect>map(field -> new FieldWrapper<>(field, requiredColumns(aggregation.dates())))
				.toList();
		IntervalPackingSelectPreparation preparation = IntervalPackingSelectCompiler.prepareArbitrarySelect(
				context.alias(),
				context.tables().getPredecessor(ConceptCteStep.AGGREGATION_SELECT),
				context.ids(),
				dates,
				dialect,
				context.nameGenerator()
		);
		ColumnDateRange packed = preparation.dateRange();
		Field<Integer> days = dialect.dateDistance(ChronoUnit.DAYS, packed.getStart(), packed.getEnd());
		FieldWrapper<BigDecimal> sum = new FieldWrapper<>(DSL.sum(
				DSL.when(packed.getStart().eq(dialect.minimumDate()).or(packed.getEnd().eq(dialect.maximumDate())), DSL.inline(null, Integer.class))
						.otherwise(days)
		).as(context.alias()));
		QueryStep predecessor = IntervalPackingSelectCompiler.compileArbitrarySelect(
				preparation.predecessor(), packed, sum, preparation.tables(), dialect
		);
		return CommonAggregationSelect.<BigDecimal>builder()
				.rootSelects(roots)
				.groupBy(sum)
				.additionalPredecessor(predecessor)
				.build();
	}

	private static ColumnDateRange dateRange(DateColumns dates, AggregationConversionContext context) {
		if (dates instanceof DateColumns.Single single && single.column().type() == ColumnType.DATE_RANGE) {
			return context.dialect().dateRangeColumn(DSL.field(DSL.name(context.tables().getRootTable(), single.column().physicalName())));
		}
		ColumnDateRange inclusive = inclusiveDates(dates, context);
		return context.dialect().dateRange(inclusive.getStart(), inclusive.getEnd());
	}

	private static ColumnDateRange inclusiveDates(DateColumns dates, AggregationConversionContext context) {
		return switch (dates) {
			case DateColumns.Pair pair -> ColumnDateRange.of(dateField(pair.start(), context), dateField(pair.end(), context));
			case DateColumns.Single single -> {
				if (single.column().type() == ColumnType.DATE_RANGE) {
					ColumnDateRange range = dateRange(dates, context);
					yield ColumnDateRange.of(range.getStart(), context.dialect().addDays(range.getEnd(), DSL.inline(-1)));
				}
				Field<Date> date = dateField(single.column(), context);
				yield ColumnDateRange.of(date, date);
			}
		};
	}

	private static Field<Date> dateField(ResolvedColumn column, AggregationConversionContext context) {
		return DSL.field(DSL.name(context.tables().getRootTable(), column.physicalName()), Date.class);
	}

	private static String[] requiredColumns(DateColumns dates) {
		return dates.columns().stream().map(ResolvedColumn::physicalName).distinct().toArray(String[]::new);
	}
}
