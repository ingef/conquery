package com.bakdata.conquery.sql.conversion.dialect;

import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.DSL;

public class ClickHouseFunctionProvider implements SqlFunctionProvider {

	private static final String INFINITY_DATE_VALUE = "9999-12-31";
	private static final String MINUS_INFINITY_DATE_VALUE = "0001-01-01";

	@Override
	public String getMinDateExpression() {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getMaxDateExpression() {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Condition dateRestriction(ColumnDateRange dateRestriction, ColumnDateRange validityDate) {

		if (dateRestriction.isSingleColumnRange() || validityDate.isSingleColumnRange()) {
			throw new UnsupportedOperationException("ClickHouse does not support single column ranges.");
		}

		Condition dateRestrictionStartsBeforeDate = dateRestriction.getStart().lessOrEqual(validityDate.getEnd());
		Condition dateRestrictionEndsAfterDate = dateRestriction.getEnd().greaterOrEqual(validityDate.getStart());

		return dateRestrictionStartsBeforeDate.and(dateRestrictionEndsAfterDate);
	}

	@Override
	public ColumnDateRange daterange(CDateRange dateRestriction) {

		String startDateExpression = MINUS_INFINITY_DATE_VALUE;
		String endDateExpression = INFINITY_DATE_VALUE;

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

		// when aggregating date ranges, we want to treat the last day of the range as excluded,
		// so when using the date value of the end column, we add +1 day as end of the date range
		Field<Date> rangeStart = DSL.field(DSL.name(qualifier, startColumn.getName()), Date.class);
		Field<Date> rangeEnd = addDay(DSL.field(DSL.name(qualifier, endColumn.getName()), Date.class));

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
			throw new UnsupportedOperationException("ClickHouse does not support single-column date ranges.");
		}

		String datesConcatenated = Stream.of(columnDateRange.getStart(), columnDateRange.getEnd())
										 .map(" || toString(%s) || "::formatted)
										 .collect(Collectors.joining(" ',' ", "'['", "')'"));

		// encapsulate all ranges within curly braces
		return DSL.field("'{' || %s || '}'".formatted(datesConcatenated), String.class);
	}

	@Override
	public Field<Integer> dateDistance(ChronoUnit timeUnit, Name startDateColumnName, Date endDateExpression) {
		String clickHouseUnit = switch (timeUnit) {
			case DAYS -> "dd";
			case WEEKS -> "wk";
			case MONTHS -> "mm";
			case YEARS, DECADES, CENTURIES -> "yy";
			default -> throw new UnsupportedOperationException("Given ChronoUnit %s is not supported.");
		};

		Field<Date> startDate = DSL.field(startDateColumnName, Date.class);
		Field<Date> endDate = toDateField(endDateExpression.toString());
		Field<Integer> dateDiff = DSL.function("age", Integer.class, DSL.val(clickHouseUnit), startDate, endDate);

		// ClickHouse does not support decades or centuries directly
		dateDiff = switch (timeUnit) {
			case DECADES -> dateDiff.divide(10);
			case CENTURIES -> dateDiff.divide(100);
			default -> dateDiff;
		};

		return dateDiff.cast(Integer.class);
	}

	@Override
	public Field<Date> addDays(Field<Date> dateColumn, int amountOfDays) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Field<Date> toDateField(String dateExpression) {
		return DSL.function(
				"toDate",
				Date.class,
				DSL.val(dateExpression)
		);
	}

	@Override
	public Field<?> first(Field<?> field, List<Field<?>> orderByColumn) {
		return DSL.field(DSL.sql("first_value({0})", field));
	}

	@Override
	public Field<?> last(Field<?> column, List<Field<?>> orderByColumns) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Field<?> random(Field<?> column) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Field<Object> prefixStringAggregation(Field<Object> field, String prefix) {
		Field<String> likePattern = DSL.inline(prefix + "%");
		String sqlTemplate = "IF("
							 + "empty(groupArray(CASE WHEN {0} LIKE {1} THEN {0} ELSE NULL END))"
							 + ", NULL"
							 + ", '[' || arrayStringConcat(groupArray(CASE WHEN {0} LIKE {1} THEN {0} ELSE NULL END), ', ') || ']'"
							 + ")";
		return DSL.field(DSL.sql(sqlTemplate, field, likePattern));
	}

	private Field<Date> addDay(Field<Date> dateColumn) {
		return DSL.function(
				"addDays",
				Date.class,
				dateColumn,
				DSL.val(1)
		);
	}

}
