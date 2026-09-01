package com.bakdata.conquery.sql.conversion.dialect.clickhouse;

import static org.jooq.impl.DSL.*;

import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.DaterangeSelectOrFilter;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import org.jetbrains.annotations.NotNull;
import org.jooq.Condition;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.SortField;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public class ClickhouseFunctionProvider implements SqlFunctionProvider {

    public static final Integer MIN_DATE_VALUE = -25567;
    public static final Integer MAX_DATE_VALUE = 24855;
    private static final String ANY_CHAR_REGEX = ".*";

    @Override
    public String getAnyCharRegex() {
        return ANY_CHAR_REGEX;
    }

    @Override
    public Field<?> asArrayRepr(List<String> value) {
        return array(value.toArray());
    }


    @Override
    public Field<String> externalId(String id) {
        // This is only used in CQExternal to define external Ids.
        // Without explicitly tagging this value as Nullable, the Clickhouse QueryPlanner assumes that this it can never be null.
        // But in the case of an anti-join (our Negation), this field will be sometimes null, while not testing as `is null` = true
        return field("{0}::Nullable(String)", String.class, inline(id, String.class));
    }

    @Override
    public Condition dateRestriction(ColumnDateRange dateRestriction, ColumnDateRange daterange) {
        Condition dateRestrictionStartsBeforeDate = dateRestriction.getStart().lessThan(daterange.getEnd());
        Condition dateRestrictionEndsAfterDate = dateRestriction.getEnd().greaterThan(daterange.getStart());

        return condition(dateRestrictionStartsBeforeDate.and(dateRestrictionEndsAfterDate));
    }


    @Override
    public ColumnDateRange forCDateRange(CDateRange daterange) {

        Field<Date> startDateExpression = getMinDateExpression();
        Field<Date> endDateExpression = getMaxDateExpression();

        if (daterange.hasLowerBound()) {
            startDateExpression = inline(Date.valueOf(daterange.getMin()));
        }
        if (daterange.hasUpperBound()) {
            endDateExpression = inline(Date.valueOf(daterange.getMax().plusDays(1)));
        }

        return ColumnDateRange.of(startDateExpression, endDateExpression);
    }

    @Override
    public Field<Date> toDateField(String dateExpression) {
        return function(
                "toDate",
                Date.class,
                inline(dateExpression),
                inline(DEFAULT_DATE_FORMAT)
        );
    }

    @Override
    public Condition isNotEmptyDateRange(ColumnDateRange columnDateRange) {
        Condition startNotMin = columnDateRange.getStart().notEqual(getMinDateExpression());
        Condition endNotMax = columnDateRange.getEnd().notEqual(getMaxDateExpression());
        return condition(startNotMin.and(endNotMax).neg());
    }

	@Override
    public ColumnDateRange forValidityDate(ValidityDate validityDate) {
        return toColumnDateRange(validityDate);
    }


    @Override
    public ColumnDateRange allRange() {
        return ColumnDateRange.of(getMinDateExpression(), getMaxDateExpression());
    }

    @Override
    public <T> Field<T> anyValue(Field<T> field) {
        return DSL.anyValue(field);
    }

    private ColumnDateRange toColumnDateRange(ValidityDate validityDate) {

        String tableName = validityDate.getConnector().resolveTableId().getTable();

        Column startColumn;
        Column endColumn;

        // if no end column is present, the only existing column is both start and end of the date range
        if (validityDate.isSingleColumnDaterange()) {
            Column column = validityDate.getColumn().resolve();
            startColumn = column;
            endColumn = column;
        } else {
            startColumn = validityDate.getStartColumn().resolve();
            endColumn = validityDate.getEndColumn().resolve();
        }

        return ofStartAndEnd(tableName, startColumn, endColumn);
    }


    @Override
    public Field<?> arrayOut(List<Field<String>> fields) {
        return field("arrayFilter(x -> x <> '', {0})", Object.class, array(fields));
    }

    private ColumnDateRange ofStartAndEnd(String tableName, Column startColumn, Column endColumn) {

        // Since coalesce makes Clickhouse certain, that the field is not nullable, it will do silly stuff with it down the line:
        // missing values (for example in outer-joins) will be coerced to 0 = 01-01-1970, which is clearly not correct
        // Therefore we tag the values as Nullable again to make Clickhouse show some respect

        Field<Date> rangeStart = field("{0}::Nullable(Date32)", Date.class, coalesce(
                        field(name(tableName, startColumn.getName()), Date.class),
                        getMinDateExpression()
                )
        );
        // when aggregating date ranges, we want to treat the last day of the range as excluded,
        // so when using the date value of the end column, we add +1 day as end of the date range
        Field<Date> rangeEnd = field("{0}::Nullable(Date32)", Date.class, coalesce(
                        addDays(field(name(tableName, endColumn.getName()), Date.class), inline(1)),
                        getMaxDateExpression()
                )
        );

        return ColumnDateRange.of(rangeStart, rangeEnd);
    }

    @Override
    public Field<Date> addDays(Field<Date> dateColumn, Field<Integer> amountOfDays) {
        return function(
                "addDays",
                Date.class,
                dateColumn,
                amountOfDays
        );
    }

    @Override
    public ColumnDateRange forValidityDate(ValidityDate validityDate, CDateRange dateRestriction) {

        ColumnDateRange validityDateRange = toColumnDateRange(validityDate);
        ColumnDateRange restriction = toColumnDateRange(dateRestriction);

        Field<Date> lowerBound = when(validityDateRange.getStart().lessThan(restriction.getStart()), restriction.getStart())
                .otherwise(validityDateRange.getStart());

        Field<Date> maxDate = getMinDateExpression();
        // we want to add +1 day to the end date - except when it's the max date already
        Field<Date> restrictionUpperBound = when(restriction.getEnd().eq(maxDate), maxDate).otherwise(addDays(restriction.getEnd(), inline(1)));
        Field<Date> upperBound = when(validityDateRange.getEnd().greaterThan(restriction.getEnd()), restrictionUpperBound)
                .otherwise(validityDateRange.getEnd());

        return ColumnDateRange.of(lowerBound, upperBound);
    }

    private ColumnDateRange toColumnDateRange(CDateRange dateRestriction) {

        Field<Date> startDateExpression = getMinDateExpression();
        Field<Date> endDateExpression = getMaxDateExpression();

        if (dateRestriction.hasLowerBound()) {
            startDateExpression = inline(Date.valueOf(dateRestriction.getMin()));
        }
        if (dateRestriction.hasUpperBound()) {
            endDateExpression = inline(Date.valueOf(dateRestriction.getMax()));
        }

        return ColumnDateRange.of(startDateExpression, endDateExpression);
    }

    @NotNull
    @Override
    public Collection<? extends OrderField<?>> orderByValidityDates(
            Function<Field<?>, ? extends SortField<?>> ordering,
            List<Field<?>> validityDateFields) {

        return List.of(
                ordering.apply(nullif(validityDateFields.getFirst(), getMinDateExpression())).nullsLast(),
                ordering.apply(nullif(validityDateFields.getLast(), getMaxDateExpression())).nullsLast()
        );
    }

    @Override
    public Field<Date> getMinDateExpression() {
        return field("toDate32({0})", Date.class, MIN_DATE_VALUE);
    }

    @Override
    public Field<Date> getMaxDateExpression() {
        return field("toDate32({0})", Date.class, MAX_DATE_VALUE);
    }

    @Override
    public ColumnDateRange forArbitraryDateRange(DaterangeSelectOrFilter daterangeSelectOrFilter) {
        String tableName = daterangeSelectOrFilter.getTable().getName();
        if (daterangeSelectOrFilter.isSingleColumnDaterange()) {
            Column column = daterangeSelectOrFilter.getColumn().resolve();
            return ofStartAndEnd(tableName, column, column);
        } else {
            return ofStartAndEnd(tableName, daterangeSelectOrFilter.getStartColumn().resolve(), daterangeSelectOrFilter.getEndColumn().resolve());
        }
    }

    @Override
    public ColumnDateRange aggregated(ColumnDateRange columnDateRange) {
        return ColumnDateRange.of(
                        min(columnDateRange.getStart()),
                        max(columnDateRange.getEnd())
                )
                .as(columnDateRange.getAlias());
    }

    @Override
    public ColumnDateRange toDualColumn(ColumnDateRange columnDateRange) {
        return ColumnDateRange.of(columnDateRange.getStart(), columnDateRange.getEnd());
    }

    @Override
    public ColumnDateRange intersection(ColumnDateRange left, ColumnDateRange right) {
        Field<Date> greatest = DSL.greatest(left.getStart(), right.getStart());
        Field<Date> least = DSL.least(left.getEnd(), right.getEnd());
        return ColumnDateRange.of(greatest, least);
    }

    @Override
    public QueryStep unnestDaterange(ColumnDateRange nested, QueryStep predecessor, String cteName) {
        return predecessor;
    }

    @Override
    public Field<Object[]> dateRangeAggregation(ColumnDateRange columnDateRange) {
        return field("groupArray({0})", Object[].class, dateRangeToField(columnDateRange));
    }

    @Override
    public Field<Object> dateRangeToField(ColumnDateRange columnDateRange) {
        Field<?> startDateExpression = field("{0}::Nullable(Integer)", Object.class, columnDateRange.getStart());
        Field<?> endDateExpression = field("{0}::Nullable(Integer)", Object.class, columnDateRange.getEnd());

        return function("tuple", Object.class, startDateExpression, endDateExpression);
    }

    @Override
    public <T> Field<T> cast(Field<?> field, DataType<T> type) {
        if (type == SQLDataType.VARCHAR) {
            return function("toString", type.getType(), field);
        }
        return function(
                name("CAST"),
                type.getType(),
                field("{0} AS {1}", field, keyword(type.getName()))
        );
    }

    @Override
    public Field<Integer> dateDistance(ChronoUnit datePart, Field<Date> startDate, Field<Date> endDate) {

        String unit = switch (datePart) {
            case DAYS -> "days";
            case MONTHS -> "months";
            case YEARS, DECADES, CENTURIES -> "years";
            default -> throw new UnsupportedOperationException("Given ChronoUnit %s is not supported.");
        };

        Field<Integer> dateDistance = function("age", Integer.class, inline(unit), startDate, endDate);

        // HANA does not support decades or centuries directly
        dateDistance = switch (datePart) {
            case DECADES -> dateDistance.divide(10);
            case CENTURIES -> dateDistance.divide(100);
            default -> dateDistance;
        };

        // otherwise HANA would return floating point numbers for date distances
        return dateDistance.cast(Integer.class);
    }


    @Override
    public <T> Field<T> random(Field<T> column) {
        return field(
                "groupArraySample(1)({0})[1]",
                column.getType(),
                column
        );
    }

    @Override
    public Condition likeRegex(Field<String> field, String pattern) {
        return condition(function("match", Boolean.class, field, inline(pattern)));
    }


    @Override
    public Field<String> yearQuarter(Field<Date> dateField) {
        return field("formatDateTime({0}, '%Y-Q%Q')", String.class, dateField);
    }

    @Override
    public ColumnDateRange allRangeIf(Condition condition) {
        return ColumnDateRange.of(
                when(condition.isTrue(),
                        getMinDateExpression()
                ),
                when(condition.isTrue(),
                        getMaxDateExpression()
                )
        );
    }

}
