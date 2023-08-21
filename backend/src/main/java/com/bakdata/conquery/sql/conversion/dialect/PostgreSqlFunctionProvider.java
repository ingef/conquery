package com.bakdata.conquery.sql.conversion.dialect;

import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.sql.models.ColumnDateRange;
import org.jetbrains.annotations.NotNull;
import org.jooq.Condition;
import org.jooq.DatePart;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * Provider of SQL functions for PostgresSQL.
 *
 * @see <a href="https://www.postgresql.org/docs/15/functions.html">PostgreSQL Documentation</a>
 */
public class PostgreSqlFunctionProvider implements SqlFunctionProvider {

	private static final String INFINITY_DATE_VALUE = "infinity";
	private static final String MINUS_INFINITY_DATE_VALUE = "-infinity";

	private static final Map<ChronoUnit, DatePart> DATE_CONVERSION = Map.of(
			ChronoUnit.DECADES, DatePart.DECADE,
			ChronoUnit.YEARS, DatePart.YEAR,
			ChronoUnit.DAYS, DatePart.DAY,
			ChronoUnit.MONTHS, DatePart.MONTH,
			ChronoUnit.CENTURIES, DatePart.CENTURY
	);

	@Override
	public Condition dateRestriction(ColumnDateRange dateRestriction, ColumnDateRange validityDate) {
		if (!validityDate.isSingleColumnRange()) {
			throw new UnsupportedOperationException("The validity date range has to be converted to a daterange field in the preprocessing step.");
		}
		else {
			// the && operator checks if two ranges overlap (see https://www.postgresql.org/docs/15/functions-range.html)
			return DSL.condition(
					"{0} && {1}",
					dateRestriction.getRange(),
					validityDate.getRange()
			);
		}
	}

	@Override
	public ColumnDateRange daterange(CDateRange dateRestriction) {

		String min = MINUS_INFINITY_DATE_VALUE;
		String max = INFINITY_DATE_VALUE;

		if (dateRestriction.hasLowerBound()) {
			min = dateRestriction.getMin().toString();
		}
		if (dateRestriction.hasUpperBound()) {
			max = dateRestriction.getMax().toString();
		}

		Field<Object> dateRestrictionRange = DSL.field(
				"daterange({0}::date, {1}::date, '[]')",
				DSL.val(min),
				DSL.val(max)
		);

		return ColumnDateRange.of(dateRestrictionRange)
							  .asDateRestrictionRange();
	}

	@Override
	public ColumnDateRange daterange(ValidityDate validityDate, String alias) {

		Field<Object> dateRange;

		if (validityDate.getEndColumn() != null) {

			Column startColumn = validityDate.getStartColumn();
			Column endColumn = validityDate.getEndColumn();

			dateRange = daterange(startColumn, endColumn, "[]");
		}
		else {
			Column column = validityDate.getColumn();
			dateRange = switch (column.getType()) {
				// if validityDateColumn is a DATE_RANGE we can make use of Postgres' integrated daterange type.
				case DATE_RANGE -> DSL.field(DSL.name(column.getName()));
				// if the validity date column is not of daterange type, we construct it manually
				case DATE -> daterange(column, column, "[]");
				default -> throw new IllegalArgumentException(
						"Given column type '%s' can't be converted to a proper date restriction.".formatted(column.getType())
				);
			};
		}

		return ColumnDateRange.of(dateRange)
							  .asValidityDateRange(alias);
	}

	@Override
	public Field<Object> daterangeString(ColumnDateRange columnDateRange) {
		if (!columnDateRange.isSingleColumnRange()) {
			throw new UnsupportedOperationException("All column date ranges should have been converted to single column ranges.");
		}
		return columnDateRange.getRange();
	}

	@Override
	public Field<Integer> dateDistance(ChronoUnit timeUnit, Column startDateColumn, Date endDateExpression) {

		DatePart datePart = DATE_CONVERSION.get(timeUnit);
		if (datePart == null) {
			throw new UnsupportedOperationException("Chrono unit %s is not supported".formatted(timeUnit));
		}

		// we can now safely cast to Field of type Date
		Field<Date> startDate = DSL.field(DSL.name(startDateColumn.getName()), Date.class);
		return DSL.dateDiff(datePart, startDate, endDateExpression);
	}

	@NotNull
	private static Field<Object> daterange(Column startColumn, Column endColumn, String bounds) {
		return DSL.function(
				"daterange",
				Object.class,
				DSL.field(DSL.name(startColumn.getName())),
				DSL.field(DSL.name(endColumn.getName())),
				DSL.val(bounds)
		);
	}

}
