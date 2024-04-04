package com.bakdata.conquery.sql.conversion.dialect;

import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import org.jooq.Condition;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Param;
import org.jooq.impl.DSL;

class HanaSqlFunctionProvider implements SqlFunctionProvider {

	private static final char DELIMITER = ',';
	private static final String MAX_DATE_VALUE = "9999-12-31";
	private static final String MIN_DATE_VALUE = "0001-01-01";
	private static final String ANY_CHAR_REGEX = ".*";

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
	public Condition dateRestriction(ColumnDateRange dateRestriction, ColumnDateRange validityDate) {

		if (dateRestriction.isSingleColumnRange() || validityDate.isSingleColumnRange()) {
			throw new UnsupportedOperationException("HANA does not support single column ranges.");
		}

		Condition dateRestrictionStartsBeforeDate = dateRestriction.getStart().lessOrEqual(validityDate.getEnd());
		Condition dateRestrictionEndsAfterDate = dateRestriction.getEnd().greaterOrEqual(validityDate.getStart());

		return DSL.condition(dateRestrictionStartsBeforeDate.and(dateRestrictionEndsAfterDate));
	}

	@Override
	public ColumnDateRange forDateRestriction(CDateRange dateRestriction) {
		return toColumnDateRange(dateRestriction).asDateRestrictionRange();
	}

	@Override
	public ColumnDateRange forTablesValidityDate(CQTable cqTable, String alias) {
		return toColumnDateRange(cqTable).asValidityDateRange(alias);
	}

	@Override
	public ColumnDateRange forTablesValidityDate(CQTable cqTable, CDateRange dateRestriction, String alias) {

		ColumnDateRange validityDate = toColumnDateRange(cqTable);
		ColumnDateRange restriction = toColumnDateRange(dateRestriction);

		Field<Date> lowerBound = DSL.when(validityDate.getStart().lessThan(restriction.getStart()), restriction.getStart())
									.otherwise(validityDate.getStart());

		Field<Date> maxDate = toDateField(MAX_DATE_VALUE); // we want to add +1 day to the end date - except when it's the max date already
		Field<Date> restrictionUpperBound = DSL.when(restriction.getEnd().eq(maxDate), maxDate).otherwise(addDays(restriction.getEnd(), 1));
		Field<Date> upperBound = DSL.when(validityDate.getEnd().greaterThan(restriction.getEnd()), restrictionUpperBound)
									.otherwise(validityDate.getEnd());

		return ColumnDateRange.of(lowerBound, upperBound).as(alias);
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
	public ColumnDateRange toDualColumn(ColumnDateRange columnDateRange) {
		// HANA does not support single column ranges
		return ColumnDateRange.of(columnDateRange.getStart(), columnDateRange.getEnd());
	}

	@Override
	public QueryStep unnestValidityDate(QueryStep predecessor, SqlTables sqlTables) {
		// HANA does not support single column datemultiranges
		return predecessor;
	}

	@Override
	public Field<String> daterangeStringAggregation(ColumnDateRange columnDateRange) {

		if (columnDateRange.isSingleColumnRange()) {
			throw new UnsupportedOperationException("HANA does not support single-column date ranges.");
		}

		Field<Date> startDate = columnDateRange.getStart();
		Field<Date> endDate = columnDateRange.getEnd();

		Param<Integer> dateLength = DSL.val(DEFAULT_DATE_FORMAT.length());
		Field<String> startDateExpression = toVarcharField(startDate, dateLength);
		Field<String> endDateExpression = toVarcharField(endDate, dateLength);

		Field<String> withMinDateReplaced = replace(startDateExpression, MIN_DATE_VALUE, MINUS_INFINITY_SIGN);
		Field<String> withMaxDateReplaced = replace(endDateExpression, MAX_DATE_VALUE, INFINITY_SIGN);

		// add interval braces to ranges: start is allways included, end is allways excluded except if it's the maximum/infinity date
		Field<String> enclosedMinDate = DSL.field("'[' || {0}", String.class, withMinDateReplaced);
		Field<String> enclosedMaxDate = DSL.when(withMaxDateReplaced.like(INFINITY_SIGN), DSL.field("{0} || ']'", String.class, withMaxDateReplaced))
										   .otherwise(DSL.field("{0} || ')'", String.class, withMaxDateReplaced));

		Field<String> rangeConcatenated = DSL.field("{0} || ',' || {1}", String.class, enclosedMinDate, enclosedMaxDate);

		Field<String> stringAggregation = DSL.field(
				"{0}({1}, {2} {3})",
				String.class,
				DSL.keyword("STRING_AGG"),
				rangeConcatenated,
				DSL.toChar(DELIMITER),
				DSL.orderBy(startDate)
		);

		// encapsulate all ranges (including empty ranges) within curly braces
		return DSL.when(stringAggregation.isNull(), DSL.field(DSL.val("{}")))
				  .otherwise(DSL.field("'{' || {0} || '}'", String.class, stringAggregation));
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
	public Field<Object[]> asArray(List<Field<?>> fields) {
		String arrayExpression = fields.stream()
									   .map(Field::toString)
									   .collect(Collectors.joining(", ", "array(", ")"));
		return DSL.field(arrayExpression, Object[].class);
	}

	@Override
	public Field<Date> addDays(Field<Date> dateColumn, int amountOfDays) {
		return DSL.function(
				"ADD_DAYS",
				Date.class,
				dateColumn,
				DSL.val(amountOfDays)
		);
	}

	private Field<String> toVarcharField(Field<Date> startDate, Param<Integer> dateExpressionLength) {
		return DSL.field(
				"{0}({1} {2}({3}))",
				String.class,
				DSL.keyword("CAST"),
				startDate,
				DSL.keyword("AS VARCHAR"),
				dateExpressionLength
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

	private ColumnDateRange toColumnDateRange(CQTable cqTable) {

		ValidityDate validityDate = cqTable.findValidityDate();
		String tableName = cqTable.getConnector().getTable().getName();

		Column startColumn;
		Column endColumn;

		// if no end column is present, the only existing column is both start and end of the date range
		if (validityDate.getEndColumn() == null) {
			startColumn = validityDate.getColumn();
			endColumn = validityDate.getColumn();
		}
		else {
			startColumn = validityDate.getStartColumn();
			endColumn = validityDate.getEndColumn();
		}

		Field<Date> rangeStart = DSL.coalesce(
				DSL.field(DSL.name(tableName, startColumn.getName()), Date.class),
				toDateField(MIN_DATE_VALUE)
		);
		// when aggregating date ranges, we want to treat the last day of the range as excluded,
		// so when using the date value of the end column, we add +1 day as end of the date range
		Field<Date> rangeEnd = DSL.coalesce(
				addDays(DSL.field(DSL.name(tableName, endColumn.getName()), Date.class), 1),
				toDateField(MAX_DATE_VALUE)
		);

		return ColumnDateRange.of(rangeStart, rangeEnd);
	}

}
