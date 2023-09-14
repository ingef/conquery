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

public class HanaSqlFunctionProvider implements SqlFunctionProvider {

	private static final String INFINITY_DATE_VALUE = "9999-12-31";
	private static final String MINUS_INFINITY_DATE_VALUE = "0001-01-01";

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
	public Field<Object> validityDateStringAggregation(ColumnDateRange columnDateRange) {

		if (columnDateRange.isSingleColumnRange()) {
			throw new UnsupportedOperationException("HANA does not support single-column date ranges.");
		}

		String rangeConcatenated = Stream.of(columnDateRange.getStart(), columnDateRange.getEnd())
										 .map(" || %s || "::formatted)
										 .collect(Collectors.joining(" ',' ", "'['", "')'"));

		// TODO (ja): STRING_AGG

		// encapsulate all ranges within curly braces
		return DSL.field("'{' || %s || '}'".formatted(rangeConcatenated));
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
		return DSL.field(DSL.sql("FIRST_VALUE({0} {1})", column, DSL.orderBy(orderByColumns)));
	}

	private Field<Date> addDay(Field<Date> dateColumn) {
		return DSL.function(
				"ADD_DAYS",
				Date.class,
				dateColumn,
				DSL.val(1)
		);
	}

}
