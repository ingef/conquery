package com.bakdata.conquery.sql.conversion.dialect;

import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Param;
import org.jooq.impl.DSL;

class HanaSqlFunctionProvider implements SqlFunctionProvider {

	public static final char DELIMITER = ',';
	private static final String MAX_DATE_VALUE = "9999-12-31";
	private static final String MIN_DATE_VALUE = "0001-01-01";

	@Override
	public String getMinDateExpression() {
		return MIN_DATE_VALUE;
	}

	@Override
	public String getMaxDateExpression() {
		return MAX_DATE_VALUE;
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
	public ColumnDateRange daterange(CDateRange dateRestriction) {

		String startDateExpression = MIN_DATE_VALUE;
		String endDateExpression = MAX_DATE_VALUE;

		if (dateRestriction.hasLowerBound()) {
			startDateExpression = dateRestriction.getMin().toString();
		}
		if (dateRestriction.hasUpperBound()) {
			endDateExpression = dateRestriction.getMax().toString();
		}

		return ColumnDateRange.of(toDateField(startDateExpression), toDateField(endDateExpression))
							  .asDateRestrictionRange();
	}

	@Override
	public ColumnDateRange daterange(ValidityDate validityDate, String qualifier, String conceptLabel) {

		Column startColumn;
		Column endColumn;

		if (validityDate.getEndColumn() != null) {
			startColumn = validityDate.getStartColumn();
			endColumn = validityDate.getEndColumn();
		}
		else {
			startColumn = validityDate.getColumn();
			endColumn = validityDate.getColumn();
		}

		Field<Date> rangeStart = DSL.coalesce(
				DSL.field(DSL.name(qualifier, startColumn.getName()), Date.class),
				toDateField(MIN_DATE_VALUE)
		);
		// when aggregating date ranges, we want to treat the last day of the range as excluded,
		// so when using the date value of the end column, we add +1 day as end of the date range
		Field<Date> rangeEnd = DSL.coalesce(
				addDays(DSL.field(DSL.name(qualifier, endColumn.getName()), Date.class), 1),
				toDateField(MAX_DATE_VALUE)
		);

		return ColumnDateRange.of(rangeStart, rangeEnd)
							  .asValidityDateRange(conceptLabel);
	}

	@Override
	public ColumnDateRange aggregated(ColumnDateRange columnDateRange) {
		return ColumnDateRange.of(
				DSL.min(columnDateRange.getStart()),
				DSL.max(columnDateRange.getEnd())
		);
	}

	@Override
	public Field<String> validityDateStringAggregation(ColumnDateRange columnDateRange) {

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
		Field<String> enclosedMinDate = DSL.field("'[' || %s".formatted(withMinDateReplaced), String.class);
		Field<String> enclosedMaxDate = DSL.when(withMaxDateReplaced.like(INFINITY_SIGN), DSL.field("%s || ']'".formatted(withMaxDateReplaced), String.class))
										   .otherwise(DSL.field("%s || ')'".formatted(withMaxDateReplaced), String.class));

		Field<String> rangeConcatenated = DSL.field("%s || ',' || %s".formatted(enclosedMinDate, enclosedMaxDate), String.class);

		Field<String> stringAggregation = DSL.field(
				"STRING_AGG({0}, {1} {2})",
				String.class,
				rangeConcatenated,
				DSL.toChar(DELIMITER),
				DSL.orderBy(startDate)
		);

		// encapsulate all ranges (including empty ranges) within curly braces
		return DSL.when(stringAggregation.isNull(), DSL.field(DSL.val("{}")))
				  .otherwise(DSL.field(("'{' || %s || '}'".formatted(stringAggregation)), String.class));
	}

	@Override
	public Field<Integer> dateDistance(ChronoUnit timeUnit, Name startDateColumnName, Date endDateExpression) {

		String betweenFunction = switch (timeUnit) {
			case DAYS -> "DAYS_BETWEEN";
			case MONTHS -> "MONTHS_BETWEEN";
			case YEARS, DECADES, CENTURIES -> "YEARS_BETWEEN";
			default -> throw new UnsupportedOperationException("Given ChronoUnit %s is not supported.");
		};

		Field<Date> startDate = DSL.field(startDateColumnName, Date.class);
		Field<Date> endDate = toDateField(endDateExpression.toString());
		Field<Integer> dateDistance = DSL.function(betweenFunction, Integer.class, startDate, endDate);

		// HANA does not support decades or centuries directly
		dateDistance = switch (timeUnit) {
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
	public Field<?> first(Field<?> column, List<Field<?>> orderByColumns) {
		if (orderByColumns.isEmpty()) {
			orderByColumns = List.of(column);
		}
		return DSL.field(DSL.sql("FIRST_VALUE({0} {1})", column, DSL.orderBy(orderByColumns)));
	}

	@Override
	public Field<?> last(Field<?> column, List<Field<?>> orderByColumns) {
		if (orderByColumns.isEmpty()) {
			orderByColumns = List.of(column);
		}
		return DSL.field(DSL.sql("LAST_VALUE({0} {1} DESC)", column, DSL.orderBy(orderByColumns)));
	}

	@Override
	public Field<?> random(Field<?> column) {
		return DSL.field(DSL.sql("FIRST_VALUE({0} {1})", column, DSL.orderBy(DSL.function("RAND", Object.class))));
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
		return DSL.field("CAST({0} AS VARCHAR({1}))", String.class, startDate, dateExpressionLength);
	}

}
