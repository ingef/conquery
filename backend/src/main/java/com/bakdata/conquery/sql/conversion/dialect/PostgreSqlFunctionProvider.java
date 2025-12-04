package com.bakdata.conquery.sql.conversion.dialect;

import static org.jooq.impl.DSL.*;

import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.DaterangeSelectOrFilter;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import org.jetbrains.annotations.NotNull;
import org.jooq.ArrayAggOrderByStep;
import org.jooq.Condition;
import org.jooq.DataType;
import org.jooq.DatePart;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.SortField;
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

	private static final String OPEN_RANGE = "[)";
	private static final String CLOSED_RANGE = "[]";
	private static final String INFINITY_DATE_VALUE = "infinity";
	private static final String MINUS_INFINITY_DATE_VALUE = "-infinity";
	private static final String ANY_CHAR_REGEX = "%";

	@Override
	public String getMaxDateExpression() {
		return INFINITY_DATE_VALUE;
	}

	@Override
	public String getAnyCharRegex() {
		return ANY_CHAR_REGEX;
	}

	@Override
	public Table<? extends Record> getNoOpTable() {
		return table(select(val(1))).as(name(SharedAliases.NOP_TABLE.getAlias()));
	}

	@NotNull
	@Override
	public Collection<? extends OrderField<?>> orderByValidityDates(
			Function<Field<?>, ? extends SortField<?>> ordering,
			List<Field<?>> validityDateFields) {

		return validityDateFields.stream()
								 .map(field -> nullif(field, emptyDateRange()))
								 .map(ordering)
								 .map(SortField::nullsLast)
								 .toList();
	}

	public Field<Object> emptyDateRange() {
		return field("{0}::daterange", val("empty"));
	}

	@Override
	public String getMinDateExpression() {
		return MINUS_INFINITY_DATE_VALUE;
	}

	@Override
	public Condition dateRestriction(ColumnDateRange dateRestriction, ColumnDateRange daterange) {
		// the && operator checks if two ranges overlap (see https://www.postgresql.org/docs/15/functions-range.html)
		return condition(
				"{0} && {1}",
				ensureIsSingleColumnRange(dateRestriction).getRange(),
				ensureIsSingleColumnRange(daterange).getRange()
		);
	}

	private ColumnDateRange ensureIsSingleColumnRange(ColumnDateRange daterange) {
		return daterange.isSingleColumnRange()
			   ? daterange
			   : ColumnDateRange.of(daterange(daterange.getStart(), daterange.getEnd(), OPEN_RANGE)); // end is already exclusive
	}

	public Field<?> daterange(Field<?> startColumn, Field<?> endColumn, String bounds) {
		return function(
				"daterange",
				Object.class,
				startColumn,
				endColumn,
				val(bounds)
		);
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
	public ColumnDateRange forCDateRange(CDateRange daterange) {

		String startDateExpression = MINUS_INFINITY_DATE_VALUE;
		String endDateExpression = INFINITY_DATE_VALUE;

		if (daterange.hasLowerBound()) {
			startDateExpression = daterange.getMin().toString();
		}
		if (daterange.hasUpperBound()) {
			endDateExpression = daterange.getMax().toString();
		}

		Field<?> daterangeField = daterange(val(startDateExpression), val(endDateExpression), CLOSED_RANGE);

		return ColumnDateRange.of(daterangeField);
	}

	private Field<Object> datemultirange(Field<?>... fields) {
		return function("datemultirange", Object.class, fields);
	}

	@Override
	public ColumnDateRange forValidityDate(ValidityDate validityDate) {
		// if there is no validity date, each entity has the max range {-inf/inf} as validity date
		return validityDate == null ? maxRange() : toColumnDateRange(validityDate);
	}

	private ColumnDateRange maxRange() {
		return ColumnDateRange.of(daterange(toDateField(MINUS_INFINITY_DATE_VALUE), toDateField(INFINITY_DATE_VALUE), CLOSED_RANGE));
	}

	private ColumnDateRange toColumnDateRange(ValidityDate validityDate) {
		String tableName = validityDate.getConnector().resolveTableId().getTable();

		if (validityDate.getColumn() != null) {
			return ofSingleColumn(tableName, validityDate.getColumn().resolve());
		}

		return ofStartAndEnd(tableName, validityDate.getStartColumn().resolve(), validityDate.getEndColumn().resolve());
	}

	@Override
	public Field<Date> toDateField(String dateValue) {
		return field("{0}::{1}", Date.class, val(dateValue), keyword("date"));
	}

	private ColumnDateRange ofSingleColumn(String tableName, Column column) {

		Field<?> dateRange;

		dateRange = switch (column.getType()) {
			// if validityDateColumn is a DATE_RANGE we can make use of Postgres' integrated daterange type, but the upper bound is exclusive by default
			case DATE_RANGE -> {
				Field<Object> daterange = field(name(column.getName()));
				Field<Date> withOpenLowerEnd = coalesce(lower(daterange), toDateField(MINUS_INFINITY_DATE_VALUE));
				Field<Date> withOpenUpperEnd = coalesce(upper(daterange), toDateField(INFINITY_DATE_VALUE));
				yield when(daterange.isNull(), emptyDateRange())
						.otherwise(daterange(withOpenLowerEnd, withOpenUpperEnd, OPEN_RANGE));
			}
			// if the validity date column is not of daterange type, we construct it manually
			case DATE -> {
				Field<Date> singleDate = field(name(tableName, column.getName()), Date.class);
				Field<Date> withOpenLowerEnd = coalesce(singleDate, toDateField(MINUS_INFINITY_DATE_VALUE));
				Field<Date> withOpenUpperEnd = coalesce(singleDate, toDateField(INFINITY_DATE_VALUE));
				yield when(singleDate.isNull(), emptyDateRange())
						.otherwise(daterange(withOpenLowerEnd, withOpenUpperEnd, CLOSED_RANGE));
			}
			default -> throw new IllegalArgumentException(
					"Given column type '%s' can't be converted to a proper date restriction.".formatted(column.getType())
			);
		};

		return ColumnDateRange.of(dateRange);
	}

	private ColumnDateRange ofStartAndEnd(String tableName, Column startColumn, Column endColumn) {

		Field<Object> startField = field(name(tableName, startColumn.getName()));
		Field<?> withOpenLowerEnd = coalesce(startField, toDateField(MINUS_INFINITY_DATE_VALUE));
		Field<Object> endField = field(name(tableName, endColumn.getName()));
		Field<?> withOpenUpperEnd = coalesce(endField, toDateField(INFINITY_DATE_VALUE));

		return ColumnDateRange.of(
				when(startField.isNull().and(endField.isNull()), emptyDateRange())
						.otherwise(this.daterange(withOpenLowerEnd, withOpenUpperEnd, CLOSED_RANGE))
		);
	}

	public Field<Date> lower(Field<Object> daterange) {
		return function("lower", Date.class, daterange);
	}

	public Field<Date> upper(Field<Object> daterange) {
		return function("upper", Date.class, daterange);
	}

	@Override
	public Field<?> functionParam(String name) {
		return field(name(name));
	}

	public String createFunctionStatement(Name name, List<String> params, Field<String> forConcept) {
		return """
					     CREATE OR REPLACE FUNCTION %s(%s) RETURNS TEXT
					     LANGUAGE SQL
					     RETURN
					     	%s;
				""".formatted(name, params.stream().map("%s text"::formatted).collect(Collectors.joining(", ")), forConcept)
				;
	}

	@Override
	public ColumnDateRange forValidityDate(ValidityDate validityDate, CDateRange dateRestriction) {
		// if there is no validity date, each entity has the max range {-inf/inf} as validity date
		ColumnDateRange validityDateRange = validityDate == null ? maxRange() : toColumnDateRange(validityDate);
		ColumnDateRange restriction = toColumnDateRange(dateRestriction);
		return intersection(validityDateRange, restriction);
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

		Field<?> dateRestrictionRange = daterange(toDateField(startDateExpression), toDateField(endDateExpression), CLOSED_RANGE);
		return ColumnDateRange.of(dateRestrictionRange);
	}

	@Override
	public ColumnDateRange intersection(ColumnDateRange left, ColumnDateRange right) {
		return ColumnDateRange.of(field(
				"{0} * {1}",
				ensureIsSingleColumnRange(left).getRange(),
				ensureIsSingleColumnRange(right).getRange()
		));
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

	private Field<Object> rangeAgg(ColumnDateRange columnDateRange) {
		return function("range_agg", Object.class, columnDateRange.getRange());
	}

	@Override
	public ColumnDateRange toDualColumn(ColumnDateRange columnDateRange) {
		Field<?> daterange = columnDateRange.getRange();
		Field<Date> start = function("lower", Date.class, daterange);
		Field<Date> end = function("upper", Date.class, daterange);
		return ColumnDateRange.of(start, end);
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

	private static Field<?> unnest(Field<?> multirange) {
		return function("unnest", Object.class, multirange);
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
		Field<String> aggregatedValidityDate = field("({0})::{1}", String.class, columnDateRange.getRange(), keyword("varchar"));
		return replace(aggregatedValidityDate, INFINITY_DATE_VALUE, INFINITY_SIGN);
	}

	@Override
	public Field<Integer> dateDistance(ChronoUnit datePart, Field<Date> startDate, Field<Date> endDate) {

		if (datePart == ChronoUnit.DAYS) {
			return cast(endDate.minus(startDate), SQLDataType.INTEGER);
		}

		Field<Integer> age = function("age", Integer.class, endDate, startDate);
		return switch (datePart) {
			case MONTHS -> extract(DatePart.YEAR, age).multiply(12).plus(extract(DatePart.MONTH, age));
			case YEARS -> extract(DatePart.YEAR, age);
			case DECADES -> extract(DatePart.DECADE, age);
			case CENTURIES -> extract(DatePart.CENTURY, age);
			default -> throw new UnsupportedOperationException("Given ChronoUnit %s is not supported.");
		};
	}

	@Override
	public <T> Field<T> cast(Field<?> field, DataType<T> type) {
		return DSL.cast(field, type);
	}

	public Field<Integer> extract(DatePart datePart, Field<?> timeInterval) {
		return field(
				"{0}({1} {2} {3})",
				Integer.class,
				keyword("extract"),
				keyword(datePart.toSQL()),
				keyword("from"),
				timeInterval
		);
	}

	@Override
	public Field<Date> addDays(Field<Date> dateColumn, Field<Integer> amountOfDays) {
		return dateColumn.plus(amountOfDays);
	}

	@Override
	public <T> Field<T> random(Field<T> column) {
		ArrayAggOrderByStep<Object[]> arrayAgg = arrayAgg(field(
				"{0} {1} {2}",
				column,
				keyword("ORDER BY"),
				function("random", Object.class)
		));
		return field("({0})[1]", column.getType(), arrayAgg);
	}

	@Override
	public Condition likeRegex(Field<String> field, String pattern) {
		return field.similarTo(pattern);
	}

	@Override
	public Field<String> yearQuarter(Field<Date> dateField) {
		return field(
				"{0}::varchar || '-Q' || {1}::varchar",
				String.class,
				DSL.extract(dateField, DatePart.YEAR),
				DSL.extract(dateField, DatePart.QUARTER)
		);
	}

}
