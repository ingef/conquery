package com.bakdata.conquery.sql.conversion.dialect;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.DaterangeSelectOrFilter;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import org.jooq.Condition;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public class HanaSqlFunctionProvider implements SqlFunctionProvider {

	private static final char DELIMITER = ',';
	private static final String MAX_DATE_VALUE = "9999-12-31";
	private static final String MIN_DATE_VALUE = "0001-01-01";
	private static final String ANY_CHAR_REGEX = ".*";
	private static final String NOP_TABLE = "DUMMY";

	@Override
	public String getMinDateExpression() {
		return MIN_DATE_VALUE;
	}

	@Override
	public String getMaxDateExpression() {
		return MAX_DATE_VALUE;
	}

	@Override
	public <T> Field<T> cast(Field<?> field, DataType<T> type) {
		// HANA would require an explicit length param when using CAST with varchar type, TO_VARCHAR does not require this
		if (type == SQLDataType.VARCHAR) {
			return DSL.function("TO_VARCHAR", type.getType(), field);
		}
		return DSL.function(
				"CAST",
				type.getType(),
				DSL.field("%s AS %s".formatted(field, type.getName()))
		);
	}

	@Override
	public String getAnyCharRegex() {
		return ANY_CHAR_REGEX;
	}

	@Override
	public Table<? extends Record> getNoOpTable() {
		// see https://help.sap.com/docs/SAP_DATA_HUB/e8d3e271a4554a35a5a6136d3d6af3f8/4d4b939b37b84bea8b2aa2ada640c392.html
		return DSL.table(DSL.name(NOP_TABLE));
	}

	@Override
	public Condition dateRestriction(ColumnDateRange dateRestriction, ColumnDateRange daterange) {

		if (dateRestriction.isSingleColumnRange() || daterange.isSingleColumnRange()) {
			throw new UnsupportedOperationException("HANA does not support single column ranges.");
		}

		Condition dateRestrictionStartsBeforeDate = dateRestriction.getStart().lessThan(daterange.getEnd());
		Condition dateRestrictionEndsAfterDate = dateRestriction.getEnd().greaterThan(daterange.getStart());

		return DSL.condition(dateRestrictionStartsBeforeDate.and(dateRestrictionEndsAfterDate));
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
	public List<ColumnDateRange> forCDateSet(CDateSet dateset, SharedAliases alias) {
		return dateset.asRanges().stream()
					  .map(this::forCDateRange)
					  .map(dateRange -> dateRange.as(alias.getAlias()))
					  .toList();
	}

	@Override
	public ColumnDateRange forValidityDate(ValidityDate validityDate) {
		return toColumnDateRange(validityDate);
	}

	@Override
	public ColumnDateRange forValidityDate(ValidityDate validityDate, CDateRange dateRestriction) {

		ColumnDateRange validityDateRange = toColumnDateRange(validityDate);
		ColumnDateRange restriction = toColumnDateRange(dateRestriction);

		Field<Date> lowerBound = DSL.when(validityDateRange.getStart().lessThan(restriction.getStart()), restriction.getStart())
									.otherwise(validityDateRange.getStart());

		Field<Date> maxDate = toDateField(MAX_DATE_VALUE); // we want to add +1 day to the end date - except when it's the max date already
		Field<Date> restrictionUpperBound = DSL.when(restriction.getEnd().eq(maxDate), maxDate).otherwise(addDays(restriction.getEnd(), DSL.val(1)));
		Field<Date> upperBound = DSL.when(validityDateRange.getEnd().greaterThan(restriction.getEnd()), restrictionUpperBound)
									.otherwise(validityDateRange.getEnd());

		return ColumnDateRange.of(lowerBound, upperBound);
	}

	@Override
	public ColumnDateRange forArbitraryDateRange(DaterangeSelectOrFilter daterangeSelectOrFilter) {
		String tableName = daterangeSelectOrFilter.getTable().getName();
		if (daterangeSelectOrFilter.getEndColumn() != null) {
			return ofStartAndEnd(tableName, daterangeSelectOrFilter.getStartColumn().resolve(), daterangeSelectOrFilter.getEndColumn().resolve());
		}
		Column column = daterangeSelectOrFilter.getColumn().resolve();
		return ofStartAndEnd(tableName, column, column);
	}

	@Override
	public ColumnDateRange aggregated(ColumnDateRange columnDateRange) {
		return ColumnDateRange.of(
									  DSL.min(columnDateRange.getStart()),
									  DSL.max(columnDateRange.getEnd())
							  )
							  .as(columnDateRange.getAlias());
	}

	@Override
	public ColumnDateRange nulled(ColumnDateRange columnDateRange) {
		return ColumnDateRange.of(toDateField(null), toDateField(null)).as(columnDateRange.getAlias());
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
	public Field<String> daterangeStringAggregation(ColumnDateRange columnDateRange) {

		Field<String> stringAggregation = stringAggregation(
				daterangeStringExpression(columnDateRange),
				DSL.toChar(DELIMITER),
				List.of(columnDateRange.getStart())
		);

		// encapsulate all ranges (including empty ranges) within curly braces
		return DSL.when(stringAggregation.isNull(), DSL.val("{}"))
				  .otherwise(encloseInCurlyBraces(stringAggregation));
	}

	@Override
	public Field<String> daterangeStringExpression(ColumnDateRange columnDateRange) {

		if (columnDateRange.isSingleColumnRange()) {
			throw new UnsupportedOperationException("HANA does not support single-column date ranges.");
		}

		Field<Date> startDate = columnDateRange.getStart();
		Field<Date> endDate = columnDateRange.getEnd();

		Field<String> startDateExpression = cast(startDate, SQLDataType.VARCHAR);
		Field<String> endDateExpression = cast(endDate, SQLDataType.VARCHAR);

		Field<String> withMinDateReplaced = replace(startDateExpression, MIN_DATE_VALUE, MINUS_INFINITY_SIGN);
		Field<String> withMaxDateReplaced = replace(endDateExpression, MAX_DATE_VALUE, INFINITY_SIGN);

		// add interval braces to ranges: start is allways included, end is allways excluded except if it's the maximum/infinity date
		Field<String> enclosedMinDate = DSL.field("'[' || {0}", String.class, withMinDateReplaced);
		Field<String> enclosedMaxDate = DSL.when(withMaxDateReplaced.like(INFINITY_SIGN), DSL.field("{0} || ']'", String.class, withMaxDateReplaced))
										   .otherwise(DSL.field("{0} || ')'", String.class, withMaxDateReplaced));

		return DSL.field("{0} || ',' || {1}", String.class, enclosedMinDate, enclosedMaxDate);
	}

	@Override
	public Field<Integer> dateDistance(ChronoUnit datePart, Field<Date> startDate, Field<Date> endDate) {

		String betweenFunction = switch (datePart) {
			case DAYS -> "DAYS_BETWEEN";
			case MONTHS -> "MONTHS_BETWEEN";
			case YEARS, DECADES, CENTURIES -> "YEARS_BETWEEN";
			default -> throw new UnsupportedOperationException("Given ChronoUnit %s is not supported.");
		};

		Field<Integer> dateDistance = DSL.function(betweenFunction, Integer.class, startDate, endDate);

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
	public Field<Date> toDateField(String dateExpression) {
		return DSL.function(
				"TO_DATE",
				Date.class,
				DSL.val(dateExpression),
				DSL.val(DEFAULT_DATE_FORMAT)
		);
	}

	@Override
	public <T> Field<T> first(Field<T> column, List<Field<?>> orderByColumns) {
		if (orderByColumns.isEmpty()) {
			orderByColumns = List.of(column);
		}
		return DSL.field(
				"{0}({1} {2})",
				column.getType(),
				DSL.keyword("FIRST_VALUE"),
				column,
				DSL.orderBy(orderByColumns)
		);
	}

	@Override
	public <T> Field<T> last(Field<T> column, List<Field<?>> orderByColumns) {
		if (orderByColumns.isEmpty()) {
			orderByColumns = List.of(column);
		}
		return DSL.field(
				"{0}({1} {2} {3})",
				column.getType(),
				DSL.keyword("LAST_VALUE"),
				column,
				DSL.orderBy(orderByColumns),
				DSL.keyword("DESC")
		);
	}

	@Override
	public <T> Field<T> random(Field<T> column) {
		return DSL.field(
				"{0}({1} {2})",
				column.getType(),
				DSL.keyword("FIRST_VALUE"),
				column,
				DSL.orderBy(DSL.function("RAND", Object.class))
		);
	}

	@Override
	public Condition likeRegex(Field<String> field, String pattern) {
		return DSL.condition("{0} {1} {2}", field, DSL.keyword("LIKE_REGEXPR"), pattern);
	}

	@Override
	public Field<String> yearQuarter(Field<Date> dateField) {
		return DSL.function("QUARTER", String.class, dateField);
	}

	@Override
	public Field<Date> addDays(Field<Date> dateColumn, Field<Integer> amountOfDays) {
		return DSL.function(
				"ADD_DAYS",
				Date.class,
				dateColumn,
				amountOfDays
		);
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

	private ColumnDateRange toColumnDateRange(ValidityDate validityDate) {

		String tableName = validityDate.getConnector().getResolvedTableId().getTable();

		Column startColumn;
		Column endColumn;

		// if no end column is present, the only existing column is both start and end of the date range
		if (validityDate.getEndColumn() == null) {
			Column column = validityDate.getColumn().resolve();
			startColumn = column;
			endColumn = column;
		}
		else {
			startColumn = validityDate.getStartColumn().resolve();
			endColumn = validityDate.getEndColumn().resolve();
		}

		return ofStartAndEnd(tableName, startColumn, endColumn);
	}

	private ColumnDateRange ofStartAndEnd(String tableName, Column startColumn, Column endColumn) {

		Field<Date> rangeStart = DSL.coalesce(
				DSL.field(DSL.name(tableName, startColumn.getName()), Date.class),
				toDateField(MIN_DATE_VALUE)
		);
		// when aggregating date ranges, we want to treat the last day of the range as excluded,
		// so when using the date value of the end column, we add +1 day as end of the date range
		Field<Date> rangeEnd = DSL.coalesce(
				addDays(DSL.field(DSL.name(tableName, endColumn.getName()), Date.class), DSL.val(1)),
				toDateField(MAX_DATE_VALUE)
		);

		return ColumnDateRange.of(rangeStart, rangeEnd);
	}

}
