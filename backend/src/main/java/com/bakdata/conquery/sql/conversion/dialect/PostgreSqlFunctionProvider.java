package com.bakdata.conquery.sql.conversion.dialect;

import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.google.common.base.Preconditions;
import org.jooq.ArrayAggOrderByStep;
import org.jooq.Condition;
import org.jooq.DataType;
import org.jooq.DatePart;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * Provider of SQL functions for PostgresSQL.
 *
 * @see <a href="https://www.postgresql.org/docs/15/functions.html">PostgreSQL Documentation</a>
 */
class PostgreSqlFunctionProvider implements SqlFunctionProvider {

	private static final String INFINITY_DATE_VALUE = "infinity";
	private static final String MINUS_INFINITY_DATE_VALUE = "-infinity";
	private static final String ANY_CHAR_REGEX = "%";

	@Override
	public String getMaxDateExpression() {
		return INFINITY_DATE_VALUE;
	}

	@Override
	public <T> Field<T> cast(Field<?> field, DataType<T> type) {
		return DSL.cast(field, type);
	}

	@Override
	public String getAnyCharRegex() {
		return ANY_CHAR_REGEX;
	}

	@Override
	public String getMinDateExpression() {
		return MINUS_INFINITY_DATE_VALUE;
	}

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

		Field<Object> intersection = DSL.field(
				"{0} * {1}", // intersection of both ranges
				validityDate.getRange(),
				restriction.getRange()
		);

		return ColumnDateRange.of(intersection).asValidityDateRange(alias);
	}

	@Override
	public ColumnDateRange aggregated(ColumnDateRange columnDateRange) {
		return ColumnDateRange.of(rangeAgg(columnDateRange)).as(columnDateRange.getAlias());
	}

	@Override
	public ColumnDateRange toDualColumn(ColumnDateRange columnDateRange) {
		Field<?> daterange = columnDateRange.getRange();
		Field<Date> start = DSL.function("lower", Date.class, daterange);
		Field<Date> end = DSL.function("upper", Date.class, daterange);
		return ColumnDateRange.of(start, end);
	}

	@Override
	public QueryStep unnestValidityDate(QueryStep predecessor, SqlTables sqlTables) {

		Preconditions.checkArgument(
				predecessor.getSelects().getValidityDate().isPresent(),
				"Can't create a unnest-CTE without a validity date present."
		);

		Selects predecessorSelects = predecessor.getQualifiedSelects();
		ColumnDateRange validityDate = predecessorSelects.getValidityDate().get();
		ColumnDateRange unnested = ColumnDateRange.of(unnest(validityDate.getRange()).as(validityDate.getAlias()));

		Selects selects = Selects.builder()
								 .ids(predecessor.getQualifiedSelects().getIds())
								 .validityDate(Optional.of(unnested))
								 .build();

		return QueryStep.builder()
						.cteName(sqlTables.cteName(ConceptCteStep.UNNEST_DATE))
						.selects(selects)
						.fromTable(QueryStep.toTableLike(predecessor.getCteName()))
						.build();
	}

	@Override
	public Field<String> daterangeStringAggregation(ColumnDateRange columnDateRange) {
		if (!columnDateRange.isSingleColumnRange()) {
			throw new UnsupportedOperationException("All column date ranges should have been converted to single column ranges.");
		}
		Field<Object> asMultirange = rangeAgg(columnDateRange);
		Field<String> aggregatedValidityDate = DSL.field("{0}::{1}", String.class, asMultirange, DSL.keyword("varchar"));
		return replace(aggregatedValidityDate, INFINITY_DATE_VALUE, INFINITY_SIGN);
	}

	@Override
	public Field<Integer> dateDistance(ChronoUnit datePart, Field<Date> startDate, Field<Date> endDate) {

		if (datePart == ChronoUnit.DAYS) {
			return endDate.minus(startDate).coerce(Integer.class);
		}

		Field<Integer> age = DSL.function("AGE", Integer.class, endDate, startDate);
		return switch (datePart) {
			case MONTHS -> extract(DatePart.YEAR, age).multiply(12).plus(extract(DatePart.MONTH, age));
			case YEARS -> extract(DatePart.YEAR, age);
			case DECADES -> extract(DatePart.DECADE, age);
			case CENTURIES -> extract(DatePart.CENTURY, age);
			default -> throw new UnsupportedOperationException("Given ChronoUnit %s is not supported.");
		};
	}

	@Override
	public Field<Date> addDays(Field<Date> dateColumn, int amountOfDays) {
		return dateColumn.plus(amountOfDays);
	}

	@Override
	public <T> Field<T> first(Field<T> column, List<Field<?>> orderByColumn) {
		return DSL.field("({0})[1]", column.getType(), DSL.arrayAgg(column));
	}

	@Override
	public <T> Field<T> last(Field<T> column, List<Field<?>> orderByColumns) {
		ArrayAggOrderByStep<Object[]> arrayAgg = DSL.arrayAgg(DSL.field(
																	  "{0} {1} {2} {3}",
																	  column,
																	  DSL.keyword("ORDER BY"),
																	  DSL.sql(orderByColumns.stream().map(Field::toString).collect(Collectors.joining(","))),
																	  DSL.keyword("DESC")
															  )
		);
		return DSL.field("({0})[1]", column.getType(), arrayAgg);
	}

	@Override
	public <T> Field<T> random(Field<T> column) {
		ArrayAggOrderByStep<Object[]> arrayAgg = DSL.arrayAgg(DSL.field(
				"{0} {1} {2}",
				column,
				DSL.keyword("ORDER BY"),
				DSL.function("random", Object.class)
		));
		return DSL.field("({0})[1]", column.getType(), arrayAgg);
	}

	@Override
	public Condition likeRegex(Field<String> field, String pattern) {
		return field.similarTo(pattern);
	}

	@Override
	public Field<String> yearQuarter(Field<Date> dateField) {
		return DSL.field(
				"{0}::varchar || '-Q' || {1}::varchar",
				String.class,
				DSL.extract(dateField, DatePart.YEAR),
				DSL.extract(dateField, DatePart.QUARTER)
		);
	}

	@Override
	public Field<Object[]> asArray(List<Field<?>> fields) {
		String arrayExpression = fields.stream()
									   .map(Field::toString)
									   .collect(Collectors.joining(", ", "array[", "]"));
		return DSL.field(arrayExpression, Object[].class);
	}

	@Override
	public Field<Date> toDateField(String dateValue) {
		return DSL.field("{0}::{1}", Date.class, DSL.val(dateValue), DSL.keyword("date"));
	}

	private static Field<Object> rangeAgg(ColumnDateRange columnDateRange) {
		return DSL.function("range_agg", Object.class, columnDateRange.getRange());
	}

	private static Field<Object> daterange(Field<?> startColumn, Field<?> endColumn, String bounds) {
		return DSL.function(
				"daterange",
				Object.class,
				startColumn,
				endColumn,
				DSL.val(bounds)
		);
	}

	private static Field<?> unnest(Field<?> multirange) {
		return DSL.function("unnest", Object.class, multirange);
	}

	private Field<Integer> extract(DatePart datePart, Field<Integer> timeInterval) {
		return DSL.field(
				"{0}({1} {2} {3})",
				Integer.class,
				DSL.keyword("EXTRACT"),
				DSL.keyword(datePart.toSQL()),
				DSL.keyword("FROM"),
				timeInterval
		);
	}

	private ColumnDateRange toColumnDateRange(CDateRange dateRestriction) {
		String startDateExpression = MINUS_INFINITY_DATE_VALUE;
		String endDateExpression = INFINITY_DATE_VALUE;

		if (dateRestriction.hasLowerBound()) {
			startDateExpression = dateRestriction.getMin().toString();
		}
		if (dateRestriction.hasUpperBound()) {
			endDateExpression = dateRestriction.getMax().toString();
		}

		Field<Object> dateRestrictionRange = DSL.function(
				"daterange",
				Object.class,
				toDateField(startDateExpression),
				toDateField(endDateExpression),
				DSL.val("[]")
		);

		return ColumnDateRange.of(dateRestrictionRange);
	}

	private ColumnDateRange toColumnDateRange(CQTable cqTable) {
		ValidityDate validityDate = cqTable.findValidityDate();
		String tableName = cqTable.getConnector().getTable().getName();

		Field<?> dateRange;

		if (validityDate.getEndColumn() != null) {

			Field<?> startColumn = DSL.coalesce(
					DSL.field(DSL.name(tableName, validityDate.getStartColumn().getName())),
					toDateField(MINUS_INFINITY_DATE_VALUE)
			);
			Field<?> endColumn = DSL.coalesce(
					DSL.field(DSL.name(tableName, validityDate.getEndColumn().getName())),
					toDateField(INFINITY_DATE_VALUE)
			);

			return ColumnDateRange.of(daterange(startColumn, endColumn, "[]"));
		}

		Column validityDateColumn = validityDate.getColumn();
		dateRange = switch (validityDateColumn.getType()) {
			// if validityDateColumn is a DATE_RANGE we can make use of Postgres' integrated daterange type, but the upper bound is exclusive by default
			case DATE_RANGE -> {
				Field<Object> daterange = DSL.field(DSL.name(validityDateColumn.getName()));
				Field<Date> startColumn = DSL.coalesce(
						DSL.function("lower", Date.class, daterange),
						toDateField(MINUS_INFINITY_DATE_VALUE)
				);
				Field<Date> endColumn = DSL.coalesce(
						DSL.function("upper", Date.class, daterange),
						toDateField(INFINITY_DATE_VALUE)
				);
				yield daterange(startColumn, endColumn, "[]");
			}
			// if the validity date column is not of daterange type, we construct it manually
			case DATE -> {
				Field<Date> column = DSL.field(DSL.name(tableName, validityDate.getColumn().getName()), Date.class);
				Field<Date> startColumn = DSL.coalesce(column, toDateField(MINUS_INFINITY_DATE_VALUE));
				Field<Date> endColumn = DSL.coalesce(column, toDateField(INFINITY_DATE_VALUE));
				yield daterange(startColumn, endColumn, "[]");
			}
			default -> throw new IllegalArgumentException(
					"Given column type '%s' can't be converted to a proper date restriction.".formatted(validityDateColumn.getType())
			);
		};

		return ColumnDateRange.of(dateRange);
	}

}
