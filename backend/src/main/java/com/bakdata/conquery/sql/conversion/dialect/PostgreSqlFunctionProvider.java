package com.bakdata.conquery.sql.conversion.dialect;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.DaterangeSelectOrFilter;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jooq.ArrayAggOrderByStep;
import org.jooq.Condition;
import org.jooq.DataType;
import org.jooq.DatePart;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

/**
 * Provider of SQL functions for PostgresSQL.
 *
 * @see <a href="https://www.postgresql.org/docs/15/functions.html">PostgreSQL Documentation</a>
 */
public class
PostgreSqlFunctionProvider implements SqlFunctionProvider {

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
	public Table<? extends Record> getNoOpTable() {
		return DSL.table(DSL.select(DSL.val(1))).as(DSL.name(SharedAliases.NOP_TABLE.getAlias()));
	}

	@Override
	public String getMinDateExpression() {
		return MINUS_INFINITY_DATE_VALUE;
	}

	@Override
	public Condition dateRestriction(ColumnDateRange dateRestriction, ColumnDateRange daterange) {
		// the && operator checks if two ranges overlap (see https://www.postgresql.org/docs/15/functions-range.html)
		return DSL.condition(
				"{0} && {1}",
				ensureIsSingleColumnRange(dateRestriction).getRange(),
				ensureIsSingleColumnRange(daterange).getRange()
		);
	}

	@Override
	public ColumnDateRange forCDateRange(CDateRange daterange) {

		String startDateExpression = MINUS_INFINITY_DATE_VALUE;
		String endDateExpression = INFINITY_DATE_VALUE;

		if (daterange.hasLowerBound()) {
			startDateExpression = daterange.getMin().toString();
		}
		if (daterange.hasUpperBound()) {
			endDateExpression = daterange.getMax().toString();
		}

		Field<Object> daterangeField = DSL.function(
				"daterange",
				Object.class,
				DSL.val(startDateExpression),
				DSL.val(endDateExpression),
				DSL.val("[]")
		);

		return ColumnDateRange.of(daterangeField);
	}

	@Override
	public List<ColumnDateRange> forCDateSet(CDateSet dateset, SharedAliases alias) {
		// Postgres can return a date set as a single multidaterange
		Field[] daterangeFields = dateset.asRanges().stream()
										 .map(this::forCDateRange)
										 .map(ColumnDateRange::getRange)
										 .toArray(Field[]::new);
		Field<Object> multirange = datemultirange(daterangeFields);
		return List.of(ColumnDateRange.of(multirange).as(alias.getAlias()));
	}

	@Override
	public ColumnDateRange forValidityDate(ValidityDate validityDate) {
		// if there is no validity date, each entity has the max range {-inf/inf} as validity date
		return validityDate == null ? maxRange() : toColumnDateRange(validityDate);
	}

	@Override
	public ColumnDateRange maxRange() {
		return ColumnDateRange.of(daterange(toDateField(MINUS_INFINITY_DATE_VALUE), toDateField(INFINITY_DATE_VALUE), "[]"));
	}

	@Override
	public ColumnDateRange forValidityDate(ValidityDate validityDate, CDateRange dateRestriction) {
		// if there is no validity date, each entity has the max range {-inf/inf} as validity date
		ColumnDateRange validityDateRange = validityDate == null ? maxRange() : toColumnDateRange(validityDate);
		ColumnDateRange restriction = toColumnDateRange(dateRestriction);
		return intersection(validityDateRange, restriction);
	}

	@Override
	public ColumnDateRange forArbitraryDateRange(DaterangeSelectOrFilter daterangeSelectOrFilter) {
		String tableName = daterangeSelectOrFilter.getTable().getName();

		if (daterangeSelectOrFilter.getColumn() != null) {
			return ofSingleColumn(tableName, daterangeSelectOrFilter.getColumn().resolve());
		}

		return ofStartAndEnd(tableName, daterangeSelectOrFilter.getStartColumn().resolve(), daterangeSelectOrFilter.getEndColumn().resolve());
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
	public ColumnDateRange intersection(ColumnDateRange left, ColumnDateRange right) {
		return ColumnDateRange.of(DSL.field(
				"{0} * {1}",
				ensureIsSingleColumnRange(left).getRange(),
				ensureIsSingleColumnRange(right).getRange()
		));
	}

	@Override
	public QueryStep unnestDaterange(ColumnDateRange nested, QueryStep predecessor, String cteName) {

		ColumnDateRange qualifiedRange = nested.qualify(predecessor.getCteName());
		ColumnDateRange unnested = ColumnDateRange.of(unnest(qualifiedRange.getRange()).as(qualifiedRange.getAlias()));

		Selects selects = Selects.builder()
								 .ids(predecessor.getQualifiedSelects().getIds())
								 .validityDate(Optional.of(unnested))
								 .build();

		return QueryStep.builder()
						.cteName(cteName)
						.selects(selects)
						.fromTable(QueryStep.toTableLike(predecessor.getCteName()))
						.build();
	}

	@Override
	public Field<String> daterangeStringAggregation(ColumnDateRange columnDateRange) {
		Field<Object> asMultirange = rangeAgg(columnDateRange);
		return daterangeStringExpression(ColumnDateRange.of(asMultirange));
	}

	@Override
	public Field<String> daterangeStringExpression(ColumnDateRange columnDateRange) {
		if (!columnDateRange.isSingleColumnRange()) {
			throw new UnsupportedOperationException("All column date ranges should have been converted to single column ranges.");
		}
		Field<String> aggregatedValidityDate = DSL.field("({0})::{1}", String.class, columnDateRange.getRange(), DSL.keyword("varchar"));
		return replace(aggregatedValidityDate, INFINITY_DATE_VALUE, INFINITY_SIGN);
	}

	@Override
	public Field<Integer> dateDistance(ChronoUnit datePart, Field<Date> startDate, Field<Date> endDate) {

		if (datePart == ChronoUnit.DAYS) {
			return cast(endDate.minus(startDate), SQLDataType.INTEGER);
		}

		Field<Integer> age = DSL.function("age", Integer.class, endDate, startDate);
		return switch (datePart) {
			case MONTHS -> extract(DatePart.YEAR, age).multiply(12).plus(extract(DatePart.MONTH, age));
			case YEARS -> extract(DatePart.YEAR, age);
			case DECADES -> extract(DatePart.DECADE, age);
			case CENTURIES -> extract(DatePart.CENTURY, age);
			default -> throw new UnsupportedOperationException("Given ChronoUnit %s is not supported.");
		};
	}

	@Override
	public Field<Date> addDays(Field<Date> dateColumn, Field<Integer> amountOfDays) {
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
	public Field<Date> toDateField(String dateValue) {
		return DSL.field("{0}::{1}", Date.class, DSL.val(dateValue), DSL.keyword("date"));
	}

	public Field<?> daterange(Field<?> startColumn, Field<?> endColumn, String bounds) {
		return DSL.function(
				"daterange",
				Object.class,
				startColumn,
				endColumn,
				DSL.val(bounds)
		);
	}

	public Field<Integer> extract(DatePart datePart, Field<?> timeInterval) {
		return DSL.field(
				"{0}({1} {2} {3})",
				Integer.class,
				DSL.keyword("extract"),
				DSL.keyword(datePart.toSQL()),
				DSL.keyword("from"),
				timeInterval
		);
	}

	private static Field<Object> emptyDateRange() {
		return DSL.field("{0}::daterange", DSL.val("empty"));
	}

	private Field<Object> rangeAgg(ColumnDateRange columnDateRange) {
		return DSL.function("range_agg", Object.class, columnDateRange.getRange());
	}

	private Field<Object> datemultirange(Field<?>... fields) {
		return DSL.function("datemultirange", Object.class, fields);
	}

	private static Field<?> unnest(Field<?> multirange) {
		return DSL.function("unnest", Object.class, multirange);
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

	private ColumnDateRange toColumnDateRange(ValidityDate validityDate) {
		String tableName = validityDate.getConnector().getResolvedTableId().getTable();

		if (validityDate.getColumn() != null) {
			return ofSingleColumn(tableName, validityDate.getColumn().resolve());
		}

		return ofStartAndEnd(tableName, validityDate.getStartColumn().resolve(), validityDate.getEndColumn().resolve());
	}

	private ColumnDateRange ofSingleColumn(String tableName, Column column) {

		Field<?> dateRange;

		dateRange = switch (column.getType()) {
			// if validityDateColumn is a DATE_RANGE we can make use of Postgres' integrated daterange type, but the upper bound is exclusive by default
			case DATE_RANGE -> {
				Field<Object> daterange = DSL.field(DSL.name(column.getName()));
				Field<Date> withOpenLowerEnd = DSL.coalesce(
						DSL.function("lower", Date.class, daterange),
						toDateField(MINUS_INFINITY_DATE_VALUE)
				);
				Field<Date> withOpenUpperEnd = DSL.coalesce(
						DSL.function("upper", Date.class, daterange),
						toDateField(INFINITY_DATE_VALUE)
				);
				yield DSL.when(daterange.isNull(), emptyDateRange())
						.otherwise(daterange(withOpenLowerEnd, withOpenUpperEnd, "[]"));
			}
			// if the validity date column is not of daterange type, we construct it manually
			case DATE -> {
				Field<Date> singleDate = DSL.field(DSL.name(tableName, column.getName()), Date.class);
				Field<Date> withOpenLowerEnd = DSL.coalesce(singleDate, toDateField(MINUS_INFINITY_DATE_VALUE));
				Field<Date> withOpenUpperEnd = DSL.coalesce(singleDate, toDateField(INFINITY_DATE_VALUE));
				yield DSL.when(singleDate.isNull(), emptyDateRange())
						.otherwise(daterange(withOpenLowerEnd, withOpenUpperEnd, "[]"));
			}
			default -> throw new IllegalArgumentException(
					"Given column type '%s' can't be converted to a proper date restriction.".formatted(column.getType())
			);
		};

		return ColumnDateRange.of(dateRange);
	}

	private ColumnDateRange ofStartAndEnd(String tableName, Column startColumn, Column endColumn) {

		Field<Object> startField = DSL.field(DSL.name(tableName, startColumn.getName()));
		Field<?> withOpenLowerEnd = DSL.coalesce(startField, toDateField(MINUS_INFINITY_DATE_VALUE));
		Field<Object> endField = DSL.field(DSL.name(tableName, endColumn.getName()));
		Field<?> withOpenUpperEnd = DSL.coalesce(endField, toDateField(INFINITY_DATE_VALUE));

		return ColumnDateRange.of(
				DSL.when(startField.isNull().and(endField.isNull()), emptyDateRange())
						.otherwise(this.daterange(withOpenLowerEnd, withOpenUpperEnd, "[]"))
		);
	}

	private ColumnDateRange ensureIsSingleColumnRange(ColumnDateRange daterange) {
		return daterange.isSingleColumnRange()
			   ? daterange
			   : ColumnDateRange.of(daterange(daterange.getStart(), daterange.getEnd(), "[)")); // end is already exclusive
	}

}
