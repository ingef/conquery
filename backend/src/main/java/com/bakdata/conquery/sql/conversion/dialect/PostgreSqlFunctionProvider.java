package com.bakdata.conquery.sql.conversion.dialect;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * Provider of SQL functions for PostgresSQL.
 *
 * @see <a href="https://www.postgresql.org/docs/15/functions.html">PostgreSQL Documentation</a>
 */
public class PostgreSqlFunctionProvider implements SqlFunctionProvider {

	@Override
	public Condition dateRestriction(Field<Object> dateRestrictionColumn, Field<Object> validityDateColumn) {
		// the && operator checks if two ranges overlap (see https://www.postgresql.org/docs/15/functions-range.html)
		return DSL.condition(
				"{0} && {1}",
				dateRestrictionColumn,
				validityDateColumn
		);
	}

	@Override
	public Field<Object> daterange(CDateRange dateRestriction) {
		return DSL.field(
				"daterange({0}::date, {1}::date, '[]')",
				DSL.val(dateRestriction.getMin().toString()),
				DSL.val(dateRestriction.getMax().toString())
		);
	}

	@Override
	public Field<Object> daterange(Column column) {
		return switch (column.getType()) {
			// if validityDateColumn is a DATE_RANGE we can make use of Postgres' integrated daterange type.
			case DATE_RANGE -> DSL.field(column.getName());
			// if the validity date column is not of daterange type, we construct it manually
			case DATE -> DSL.field(
					"daterange({0}, {0}, '[]')",
					DSL.field(column.getName())
			);
			default -> throw new IllegalArgumentException(
					"Given column type '%s' can't be converted to a proper date restriction."
							.formatted(column.getType())
			);
		};
	}

}
