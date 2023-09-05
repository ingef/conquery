package com.bakdata.conquery.sql.conversion.dialect;

import java.sql.Date;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.sql.models.ColumnDateRange;
import org.jooq.Condition;
import org.jooq.DatePart;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.DSL;

/**
 * Provider of SQL functions for PostgresSQL.
 *
 * @see <a href="https://www.postgresql.org/docs/15/functions.html">PostgreSQL Documentation</a>
 */
public class PostgreSqlFunctionProvider implements SqlFunctionProvider {

	private static final String INFINITY_DATE_VALUE = "infinity";
	private static final String MINUS_INFINITY_DATE_VALUE = "-infinity";

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

		String startDateExpression = MINUS_INFINITY_DATE_VALUE;
		String endDateExpression = INFINITY_DATE_VALUE;

		if (dateRestriction.hasLowerBound()) {
			startDateExpression = dateRestriction.getMin().toString();
		}
		if (dateRestriction.hasUpperBound()) {
			endDateExpression = dateRestriction.getMax().toString();
		}

		Field<Object> dateRestrictionRange = DSL.field(
				"daterange({0}::date, {1}::date, '[]')",
				DSL.val(startDateExpression),
				DSL.val(endDateExpression)
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
	public Field<Integer> dateDistance(ChronoUnit timeUnit, Name startDateColumnName, Date endDateExpression) {

		Field<Date> startDate = DSL.field(startDateColumnName, Date.class);
		Field<Date> endDate = toDateField(endDateExpression.toString());

		if (timeUnit == ChronoUnit.DAYS) {
			return endDate.minus(startDate).coerce(Integer.class);
		}

		Field<Object> age = DSL.function("AGE", Object.class, endDate, startDate);

		return switch (timeUnit) {
			case MONTHS -> extract(DatePart.YEAR, age).multiply(12)
													  .plus(extract(DatePart.MONTH, age));
			case YEARS -> extract(DatePart.YEAR, age);
			case DECADES -> extract(DatePart.DECADE, age);
			case CENTURIES -> extract(DatePart.CENTURY, age);
			default -> throw new UnsupportedOperationException("Given ChronoUnit %s is not supported.");
		};
	}

	private Field<Object> daterange(Column startColumn, Column endColumn, String bounds) {
		return DSL.function(
				"daterange",
				Object.class,
				DSL.field(DSL.name(startColumn.getName())),
				DSL.field(DSL.name(endColumn.getName())),
				DSL.val(bounds)
		);
	}

	private Field<Integer> extract(DatePart datePart, Field<Object> timeInterval) {
		return DSL.function(
				"EXTRACT",
				Integer.class,
				DSL.inlined(DSL.field("%s FROM %s".formatted(datePart, timeInterval)))
		);
	}

}
