package com.bakdata.conquery.sql.compiler.dialect.hana;

import static com.bakdata.conquery.sql.compiler.dialect.Interval.MONTHS_PER_QUARTER;
import static org.jooq.impl.DSL.*;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.model.range.DateRange;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.jooq.Condition;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;

/** HANA SQL capabilities used by the framework-neutral compiler. */
public class HanaCompilerDialect implements CompilerDialect {

	@Override
	public HanaStratificationFunctions stratificationFunctions() {
		return new HanaStratificationFunctions(this);
	}

  public static final String MAX_DATE_VALUE = "9999-12-31";
  public static final String MIN_DATE_VALUE = "0001-01-01";
  public static final String DATERANGE_SEPARATOR = "/";
  private static final char DATE_SET_SEPARATOR = (char) 31;

  @Override
  public int getNameMaxLength() {
    return 127;
  }

  @Override
  public String regexAnyCharacters() {
    return ".*";
  }

  @Override
  public Condition regexMatches(Field<String> value, String pattern) {
    return condition("{0} {1} {2}", value, keyword("LIKE_REGEXPR"), pattern);
  }

  @Override
  public Table<? extends Record> literalSelectTable() {
    return table(name("DUMMY"));
  }

  @Override
  public Condition unconditionalJoinCondition() {
    return inline(true).eq(inline(true));
  }

  public Field<Date> toDateField(String expression) {
    return function("TO_DATE", Date.class, inline(expression), inline("yyyy-mm-dd"));
  }

  @Override
  public Field<Date> minimumDate() {
    return toDateField(MIN_DATE_VALUE);
  }

  @Override
  public Field<Date> maximumDate() {
    return toDateField(MAX_DATE_VALUE);
  }

  @Override
  public <T> Field<T> anyValue(Field<T> value) {
    return min(value);
  }

  @Override
  public Condition orAgg(Field<Boolean> value) {
    return condition(max(value.cast(Integer.class)).gt(0));
  }

  @Override
  public Field<?> arrayOut(List<Field<String>> fields) {
    return field(
        fields.stream()
            .map(Field::toString)
            .collect(java.util.stream.Collectors.joining(" || '" + DATE_SET_SEPARATOR + "' || ")),
        String.class);
  }

  @Override
  public Field<?> externalStringValues(List<String> values) {
    return field(
        values.stream()
            .map(org.jooq.impl.DSL::inline)
            .map(Field::toString)
            .collect(java.util.stream.Collectors.joining(" || '" + DATE_SET_SEPARATOR + "' || ")),
        Object.class);
  }

  @Override
  public <T> Field<T> cast(Field<?> value, DataType<T> type) {
    if (type == SQLDataType.VARCHAR) return function("TO_VARCHAR", type.getType(), value);
    return function(
        unquotedName("CAST"), type.getType(), field("{0} AS {1}", value, keyword(type.getName())));
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
  public Field<Integer> dateDistance(ChronoUnit unit, Field<Date> start, LocalDate end) {
    return dateDistance(unit, start, toDateField(end.toString()));
  }

  @Override
  public ColumnDateRange dateRangeLiteral(DateRange range) {
    Field<Date> start =
        range
            .startInclusive()
            .<Field<Date>>map(value -> toDateField(value.toString()))
            .orElseGet(this::minimumDate);
    Field<Date> end =
        range
            .endInclusive()
            .<Field<Date>>map(value -> toDateField(value.plusDays(1).toString()))
            .orElseGet(this::maximumDate);
    return ColumnDateRange.of(start, end);
  }

  @Override
  public Field<Integer> dateDistance(ChronoUnit unit, Field<Date> start, Field<Date> end) {
    String functionName =
        switch (unit) {
          case DAYS -> "DAYS_BETWEEN";
          case MONTHS -> "MONTHS_BETWEEN";
          case YEARS, DECADES, CENTURIES -> "YEARS_BETWEEN";
          default ->
              throw new UnsupportedOperationException(
                  "Given ChronoUnit %s is not supported.".formatted(unit));
        };
    Field<Integer> distance = function(functionName, Integer.class, start, end);
    distance =
        switch (unit) {
          case DECADES -> distance.divide(10);
          case CENTURIES -> distance.divide(100);
          default -> distance;
        };
    return distance.cast(Integer.class);
  }

  @Override
  public Field<Date> addDays(Field<Date> date, Field<Integer> days) {
    return function("ADD_DAYS", Date.class, date, days);
  }

  @Override
  public <T> Field<T> random(Field<T> value) {
    return field(
        "{0}({1} {2})",
        value.getType(), keyword("FIRST_VALUE"), value, orderBy(function("RAND", Object.class)));
  }

  @Override
  public Field<String> yearQuarter(Field<Date> date) {
    return function("QUARTER", String.class, date);
  }

  @Override
  public Field<Date> quarterStart(Field<Date> date) {
    Field<Integer> quarter =
        cast(function("RIGHT", String.class, yearQuarter(date), inline(1)), SQLDataType.INTEGER);
    return function(
        "ADD_MONTHS", Date.class, yearStart(date), quarter.minus(1).times(MONTHS_PER_QUARTER));
  }

  @Override
  public Field<Date> nextQuarterStart(Field<Date> date) {
    Field<Integer> quarter =
        cast(function("RIGHT", String.class, yearQuarter(date), inline(1)), SQLDataType.INTEGER);
    return function("ADD_MONTHS", Date.class, yearStart(date), quarter.times(MONTHS_PER_QUARTER));
  }

  private static Field<Date> yearStart(Field<Date> date) {
    return field(
        "SERIES_ROUND({0}, {1}, {2})",
        Date.class, date, inline("INTERVAL 1 YEAR"), keyword("ROUND_DOWN"));
  }

  @Override
  public Field<?> renderDateRange(Field<Date> start, Field<Date> end) {
    return field(
        "'[' || {0} || {2} || {1} || ')'",
        String.class,
        cast(start, SQLDataType.VARCHAR),
        cast(end, SQLDataType.VARCHAR),
        DATERANGE_SEPARATOR);
  }

  @Override
  public Field<?> aggregateDateRanges(Field<Date> start, Field<Date> end) {
    return stringAggregation(
        (Field<String>) renderDateRange(start, end),
        toChar(DATE_SET_SEPARATOR),
        List.of(coalesce(start, minimumDate())));
  }

  @Override
  public QueryStep unnestDateRange(ColumnDateRange range, QueryStep predecessor, String cteName) {
    return predecessor;
  }
}
