package com.bakdata.conquery.sql.compiler.dialect.clickhouse;

import static com.bakdata.conquery.sql.compiler.dialect.Interval.MONTHS_PER_QUARTER;
import static org.jooq.impl.DSL.*;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.bakdata.conquery.sql.compiler.conversion.operation.SelectConversionContext;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.model.operation.BuiltInSelects;
import org.jooq.Condition;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.SortField;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

/** ClickHouse SQL capabilities used by the framework-neutral compiler. */
public class ClickhouseCompilerDialect implements CompilerDialect {

	@Override
	public ClickhouseStratificationFunctions stratificationFunctions() {
		return new ClickhouseStratificationFunctions(this);
	}

  public static final int MIN_DATE_VALUE = -25567;
  public static final int MAX_DATE_VALUE = 24855;

  @Override
  public int getNameMaxLength() {
    return 64;
  }

  @Override
  public Field<Date> minimumDate() {
    return field("toDate32({0})", Date.class, MIN_DATE_VALUE);
  }

  @Override
  public Field<Date> maximumDate() {
    return field("toDate32({0})", Date.class, MAX_DATE_VALUE);
  }

  @Override
  public Condition isNotEmptyDateRange(ColumnDateRange range) {
    Condition startNotMin = range.getStart().notEqual(minimumDate());
    Condition endNotMax = range.getEnd().notEqual(maximumDate());
    return condition(startNotMin.and(endNotMax).neg());
  }

  @Override
  public <T> Field<T> anyValue(Field<T> value) {
    return DSL.anyValue(value);
  }

  @Override
  public Condition orAgg(Field<Boolean> value) {
    return condition(max(value.cast(Integer.class)).gt(0));
  }

  @Override
  public Field<?> externalStringValues(List<String> values) {
    return array(values.toArray());
  }

  @Override
  public ConnectorSqlSelects distinctSelect(
      BuiltInSelects.Values select, SelectConversionContext context) {
    return com.bakdata.conquery.sql.compiler.conversion.operation.ClickhouseDistinctSelectConverter
        .connectorSelect(select, context);
  }

  @Override
  public Field<String> externalId(String id) {
    return field("{0}::Nullable(String)", String.class, inline(id, String.class));
  }

  @Override
  public Field<?> arrayOut(List<Field<String>> fields) {
    return field("arrayFilter(x -> x <> '', {0})", Object.class, array(fields));
  }

  @Override
  public ColumnDateRange dateRange(Field<Date> start, Field<Date> inclusiveEnd) {
    return ColumnDateRange.of(
        field("{0}::Nullable(Date32)", Date.class, coalesce(start, minimumDate())),
        field(
            "{0}::Nullable(Date32)",
            Date.class, coalesce(addDays(inclusiveEnd, inline(1)), maximumDate())));
  }

  @Override
  public Field<Date> addDays(Field<Date> date, Field<Integer> days) {
    return function("addDays", Date.class, date, days);
  }

  @Override
  public Field<String> stringAggregation(
      Field<String> value, Field<String> delimiter, List<Field<?>> orderByFields) {
    return field(
        "{0}({1}, {2} {3})",
        String.class, keyword("string_agg"), value, delimiter, orderBy(orderByFields));
  }

  @Override
  public Collection<? extends OrderField<?>> orderByValidityDates(
      Function<Field<?>, ? extends SortField<?>> ordering, List<Field<?>> fields) {
    return List.of(
        ordering.apply(nullif(fields.getFirst(), minimumDate())).nullsLast(),
        ordering.apply(nullif(fields.getLast(), maximumDate())).nullsLast());
  }

  @Override
  public <T> Field<T> cast(Field<?> value, DataType<T> type) {
    if (type == SQLDataType.VARCHAR) return function("toString", type.getType(), value);
    return function(
        name("CAST"), type.getType(), field("{0} AS Nullable({1})", value, keyword(type.getName())));
  }

  public Field<Date> toDateField(String expression) {
    return function("toDate", Date.class, inline(expression), inline("yyyy-mm-dd"));
  }

  @Override
  public Field<Integer> dateDistance(ChronoUnit unit, Field<Date> start, LocalDate end) {
    return dateDistance(unit, start, toDateField(end.toString()));
  }

  @Override
  public Field<Integer> dateDistance(ChronoUnit unit, Field<Date> start, Field<Date> end) {
    String unitName =
        switch (unit) {
          case DAYS -> "days";
          case MONTHS -> "months";
          case YEARS, DECADES, CENTURIES -> "years";
          default ->
              throw new UnsupportedOperationException(
                  "Given ChronoUnit %s is not supported.".formatted(unit));
        };
    Field<Integer> distance = function("age", Integer.class, inline(unitName), start, end);
    distance =
        switch (unit) {
          case DECADES -> distance.divide(10);
          case CENTURIES -> distance.divide(100);
          default -> distance;
        };
    return distance.cast(Integer.class);
  }

  @Override
  public <T> Field<T> random(Field<T> value) {
    return field("groupArraySample(1)({0})[1]", value.getType(), value);
  }

  @Override
  public Condition regexMatches(Field<String> value, String pattern) {
    return condition(function("match", Boolean.class, value, inline(pattern)));
  }

  @Override
  public Field<String> yearQuarter(Field<Date> date) {
    return field("formatDateTime({0}, '%Y-Q%Q')", String.class, date);
  }

  @Override
  public Field<Date> quarterStart(Field<Date> date) {
    Field<Integer> quarter =
        cast(function("RIGHT", String.class, yearQuarter(date), inline(1)), SQLDataType.INTEGER);
    return function(
        "addMonths",
        Date.class,
        function("toStartOfYear", Date.class, date),
        quarter.minus(1).times(MONTHS_PER_QUARTER));
  }

  @Override
  public Field<Date> nextQuarterStart(Field<Date> date) {
    Field<Integer> quarter =
        cast(function("RIGHT", String.class, yearQuarter(date), inline(1)), SQLDataType.INTEGER);
    return function(
        "addMonths",
        Date.class,
        function("toStartOfYear", Date.class, date),
        quarter.times(MONTHS_PER_QUARTER));
  }

  @Override
  public Field<?> renderDateRange(Field<Date> start, Field<Date> end) {
    return function(
        "tuple",
        Object.class,
        field("{0}::Nullable(Integer)", Object.class, start),
        field("{0}::Nullable(Integer)", Object.class, end));
  }

  @Override
  public Field<?> aggregateDateRanges(Field<Date> start, Field<Date> end) {
    return field("groupArray({0})", Object[].class, renderDateRange(start, end));
  }

  @Override
  public QueryStep unnestDateRange(ColumnDateRange range, QueryStep predecessor, String cteName) {
    return predecessor;
  }
}
