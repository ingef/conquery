package com.bakdata.conquery.sql.conversion.dialect.hana;

import static com.bakdata.conquery.sql.execution.ResultSetProcessor.UNIT_SEPARATOR;
import static org.jooq.impl.DSL.*;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
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
import org.jooq.Record;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public class HanaSqlFunctionProvider implements SqlFunctionProvider {

	public static final String MAX_DATE_VALUE = "9999-12-31";
	public static final String MIN_DATE_VALUE = "0001-01-01";
	public static final String DATERANGE_SEPARATOR = "/";

	public static final char DATE_SET_SEPARATOR = UNIT_SEPARATOR;
	private static final String ANY_CHAR_REGEX = ".*";
	private static final String NOP_TABLE = "DUMMY";

	@Override
	public String getAnyCharRegex() {
		return ANY_CHAR_REGEX;
	}


	@Override
	public Table<? extends Record> getNoOpTable() {
		// see https://help.sap.com/docs/SAP_DATA_HUB/e8d3e271a4554a35a5a6136d3d6af3f8/4d4b939b37b84bea8b2aa2ada640c392.html
		return table(name(NOP_TABLE));
	}

	@Override
	public Condition unconditionalJoinCondition() {
		// Hana requires a specific syntax structure, this is the minimal solution.
		return inline(true).eq(inline(true));
	}

	@Override
	public Condition dateRestriction(ColumnDateRange dateRestriction, ColumnDateRange daterange) {
		Condition dateRestrictionStartsBeforeDate = dateRestriction.getStart().lessThan(daterange.getEnd());
		Condition dateRestrictionEndsAfterDate = dateRestriction.getEnd().greaterThan(daterange.getStart());

		return condition(dateRestrictionStartsBeforeDate.and(dateRestrictionEndsAfterDate));
	}


	@Override
	public ColumnDateRange forCDateRange(CDateRange daterange) {

		String startDateExpression = MIN_DATE_VALUE;
		String endDateExpression = MAX_DATE_VALUE;

		if (daterange.hasLowerBound()) {
			startDateExpression = daterange.getMin().toString();
		}
		if (daterange.hasUpperBound()) {
			// end date is expected to be handled as exclusive, but if it's already the maximum date, we can't add +1 day
			if (Objects.equals(daterange.getMax(), LocalDate.ofEpochDay(CDateRange.POSITIVE_INFINITY))) {
				throw new UnsupportedOperationException(
						"Given daterange has an upper bound of CDateRange.POSITIVE_INFINITY, which is not supported by ConQuery's HANA dialect.");
			}
			LocalDate exclusiveMaxDate = daterange.getMax().plusDays(1);
			endDateExpression = exclusiveMaxDate.toString();
		}

		return ColumnDateRange.of(toDateField(startDateExpression), toDateField(endDateExpression));
	}

	@Override
	public Field<Date> toDateField(String dateExpression) {
		return function(
				"TO_DATE",
				Date.class,
				inline(dateExpression),
				inline(DEFAULT_DATE_FORMAT)
		);
	}

	@Override
	public Condition isNotEmptyDateRange(ColumnDateRange columnDateRange) {
		return columnDateRange.getStart().notEqual(getMinDateExpression()).or(columnDateRange.getEnd().notEqual(getMaxDateExpression()));

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
        // Hana does not have any_value
        return DSL.min(field);
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

	private ColumnDateRange ofStartAndEnd(String tableName, Column startColumn, Column endColumn) {

		Field<Date> rangeStart = coalesce(
				field(name(tableName, startColumn.getName()), Date.class),
				getMinDateExpression()
		);
		// when aggregating date ranges, we want to treat the last day of the range as excluded,
		// so when using the date value of the end column, we add +1 day as end of the date range
		Field<Date> rangeEnd = coalesce(
				addDays(field(name(tableName, endColumn.getName()), Date.class), inline(1)),
				getMaxDateExpression()
		);

		return ColumnDateRange.of(rangeStart, rangeEnd);
	}

	@Override
	public Field<Date> addDays(Field<Date> dateColumn, Field<Integer> amountOfDays) {
		return function(
				"ADD_DAYS",
				Date.class,
				dateColumn,
				amountOfDays
		);
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


	@Override
	public ColumnDateRange forValidityDate(ValidityDate validityDate, CDateRange dateRestriction) {

		ColumnDateRange validityDateRange = toColumnDateRange(validityDate);
		ColumnDateRange restriction = toColumnDateRange(dateRestriction);

		Field<Date> lowerBound = when(validityDateRange.getStart().lessThan(restriction.getStart()), restriction.getStart())
				.otherwise(validityDateRange.getStart());

		Field<Date> maxDate = toDateField(MAX_DATE_VALUE); // we want to add +1 day to the end date - except when it's the max date already
		Field<Date> restrictionUpperBound = when(restriction.getEnd().eq(maxDate), maxDate).otherwise(addDays(restriction.getEnd(), inline(1)));
		Field<Date> upperBound = when(validityDateRange.getEnd().greaterThan(restriction.getEnd()), restrictionUpperBound)
				.otherwise(validityDateRange.getEnd());

		return ColumnDateRange.of(lowerBound, upperBound);
	}

	private ColumnDateRange toColumnDateRange(CDateRange dateRestriction) {

		String startDateExpression = MIN_DATE_VALUE;
		String endDateExpression = MAX_DATE_VALUE;

		if (dateRestriction.hasLowerBound()) {
			startDateExpression = dateRestriction.getMin().toString();
		}
		if (dateRestriction.hasUpperBound()) {
			endDateExpression = dateRestriction.getMax().toString();
		}

		return ColumnDateRange.of(toDateField(startDateExpression), toDateField(endDateExpression));
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
		return toDateField(MIN_DATE_VALUE);
	}

	@Override
	public Field<Date> getMaxDateExpression() {
		return toDateField(MAX_DATE_VALUE);
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
		// HANA does not support single column ranges
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
		// HANA does not support single column datemultiranges
		return predecessor;
	}

	@Override
	public Field<String> dateRangeAggregation(ColumnDateRange columnDateRange) {

		Field<String> stringAggregation = stringAggregation(
				dateRangeToField(columnDateRange),
				toChar(DATE_SET_SEPARATOR),
				// The coalesce is necessary so that hana isn't upset about a potential `order by null` which happens for empty date-ranges
                List.of(coalesce(columnDateRange.getStart(), getMinDateExpression()))
		);

		// encapsulate all ranges (including empty ranges) within curly braces
		return stringAggregation;
	}

	@Override
	public Field<String> dateRangeToField(ColumnDateRange columnDateRange) {
		// translation is handled in printer
		return field("'[' || {0} || {2} || {1} || ')'", String.class,
				cast(columnDateRange.getStart(), SQLDataType.VARCHAR),
				cast(columnDateRange.getEnd(), SQLDataType.VARCHAR),
				DATERANGE_SEPARATOR
		);
	}

	@Override
	public <T> Field<T> cast(Field<?> field, DataType<T> type) {
		// HANA would require an explicit length param when using CAST with varchar type, TO_VARCHAR does not require this
		if (type == SQLDataType.VARCHAR) {
			return function("TO_VARCHAR", type.getType(), field);
		}
		return function(
				// Needs to be explicitly unquoted, otherwise Hana is angry when Jooq quotes it on occasion.
				unquotedName("CAST"),
				type.getType(),
				field("{0} AS {1}", field, keyword(type.getName()))
		);
	}

	@Override
	public Field<Integer> dateDistance(ChronoUnit datePart, Field<Date> startDate, Field<Date> endDate) {

		String betweenFunction = switch (datePart) {
			case DAYS -> "DAYS_BETWEEN";
			case MONTHS -> "MONTHS_BETWEEN";
			case YEARS, DECADES, CENTURIES -> "YEARS_BETWEEN";
			default -> throw new UnsupportedOperationException("Given ChronoUnit %s is not supported.");
		};

		Field<Integer> dateDistance = function(betweenFunction, Integer.class, startDate, endDate);

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
				"{0}({1} {2})",
				column.getType(),
				keyword("FIRST_VALUE"),
				column,
				orderBy(function("RAND", Object.class))
		);
	}

	@Override
	public Condition likeRegex(Field<String> field, String pattern) {
		return condition("{0} {1} {2}", field, keyword("LIKE_REGEXPR"), pattern);
	}


	@Override
	public Field<String> yearQuarter(Field<Date> dateField) {
		return function("QUARTER", String.class, dateField);
	}

	@Override
	public Field<Boolean> isNull(Field<?> field) {
		// DSl.isNull does not work in some cases for Hana. This accomplishes the same thing with extra steps (:
		return DSL.function("IFNULL", Boolean.class, field, inline(true));
	}
}