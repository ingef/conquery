package com.bakdata.conquery.sql.conversion.dialect;


import static org.jooq.impl.DSL.*;

import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.concepts.DaterangeSelectOrFilter;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.sql.compiler.ir.SharedAliases;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import org.jooq.Condition;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.TableOnConditionStep;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

/**
 * Provider of SQL functions.
 */
public interface SqlFunctionProvider {

	String DEFAULT_DATE_FORMAT = "yyyy-mm-dd";
	String SQL_UNIT_SEPARATOR = " || '%s' || ".formatted(ResultSetProcessor.UNIT_SEPARATOR);

	/**
	 * Create database specific representation of the input list, such that it can be read by the respective {@link ResultSetProcessor}.
	 */
	default Field<?> asArrayRepr(List<String> value) {
		return field(value.stream()
				.map(DSL::inline)
				.map(Field::toString)
				.collect(Collectors.joining(SQL_UNIT_SEPARATOR)), Object.class
		);
	}

	Collection<? extends OrderField<?>> orderByValidityDates(
			Function<Field<?>, ? extends SortField<?>> ordering,
			List<Field<?>> validityDateFields);

	/**
	 * Return date-Field for the lowest representable date. This is specific per Database engine.
	 *
	 * @implSpec We assume, that this value is unreachable and therefore treat it as infinity.
	 */
	Field<Date> getMinDateExpression();

	/**
	 * Return date-Field for the highest representable date. This is specific per Database engine.
	 *
	 * @implSpec We assume, that this value is unreachable and therefore treat it as infinity.
	 */
	Field<Date> getMaxDateExpression();

	<T> Field<T> cast(Field<?> field, DataType<T> type);

	/**
	 * @return The regex that matches any char repeated any times (including 0), for example:
	 * <ul>
	 *     <li>'%' for Postgres' regexes</li>
	 *     <li>'.*' for HANA's regexes</li>
	 * </ul
	 */
	String getAnyCharRegex();

	/**
	 * @return A dummy table that enables selection of static values.
	 */
	default Table<? extends Record> getNoOpTable() {
		return noTable();
	}

	/**
	 * A date restriction condition is true if holds: dateRestrictionStart < daterangeEnd and dateRestrictionEnd > daterangeStart. The ends of both ranges are
	 * exclusive.
	 */
	Condition dateRestriction(ColumnDateRange dateRestriction, ColumnDateRange daterange);

	/**
	 * Creates a {@link ColumnDateRange} as a SQL representation of the {@link CDateRange}.
	 */
	ColumnDateRange forCDateRange(CDateRange daterange);

	/**
	 * Creates a list of {@link ColumnDateRange}s for each {@link CDateRange} of the given {@link CDateSet}. Each {@link ColumnDateRange} will be aliased with
	 * the same given {@link SharedAliases}.
	 */
	default List<ColumnDateRange> forCDateSet(CDateSet dateset, SharedAliases alias){
		if (dateset.isEmpty()) {
			// Need to explicitly provide an empty result
			return List.of(emptyColumnDateRange().as(alias.getAlias()));
		}

		return dateset.asRanges().stream()
				.map(this::forCDateRange)
				.map(dateRange -> dateRange.as(alias.getAlias()))
				.toList();
	}

	/**
	 * Creates a {@link ColumnDateRange} for a tables {@link ValidityDate}.
	 */
	ColumnDateRange forValidityDate(ValidityDate validityDate);


	/**
	 * Create condition for if the validityDate is empty.
	 * Empty means not having both start and end, having just one is acceptable.
	 */
	default Condition isNotEmptyValidityDate(ValidityDate validityDate) {
		if (validityDate.isSingleColumnDaterange()) {
			ColumnId singleColumn = validityDate.getColumn();
			return field(name(singleColumn.getTable().getTable(), singleColumn.getColumn())).isNotNull();
		}

		ColumnId startColumn = validityDate.getStartColumn();
		ColumnId endColumn = validityDate.getEndColumn();

		Condition isNotEmptyStart = field(name(startColumn.getTable().getTable(), startColumn.getColumn())).isNotNull();
		Condition isNotEmptyEnd = field(name(endColumn.getTable().getTable(), endColumn.getColumn())).isNotNull();

		return isNotEmptyStart.or(isNotEmptyEnd);
	}

	/**
	 * Creates a {@link ColumnDateRange} of maximum range.
	 */
	ColumnDateRange allRange();

	<T> Field<T> anyValue(Field<T> field);

	/**
	 * Creates a {@link ColumnDateRange} for a tables {@link CQTable}s validity date. The validity dates bounds will be restricted by the given date
	 * restriction.
	 */
	ColumnDateRange forValidityDate(ValidityDate validityDate, CDateRange dateRestriction);

	ColumnDateRange forArbitraryDateRange(DaterangeSelectOrFilter daterangeSelectOrFilter);

	/**
	 * Aggregate columnDateRange into dateSpans of the grouping.
	 */
	ColumnDateRange aggregated(ColumnDateRange columnDateRange);

	/**
	 * Given a single-column {@link ColumnDateRange}, it will create a new {@link ColumnDateRange} with a start and end field.
	 * For dialects that don't support single-column ranges, it will create a copy of the given {@link ColumnDateRange}.
	 *
	 * @return A {@link ColumnDateRange} which has a start and end field.
	 */
	ColumnDateRange toDualColumn(ColumnDateRange columnDateRange);

	/**
	 * Return {@link ColumnDateRange} containing intersection / shared time of input columns.
	 */
	ColumnDateRange intersection(ColumnDateRange left, ColumnDateRange right);


	/**
	 * @param predecessor The predeceasing step containing the aggregated {@link ColumnDateRange}.
	 * @param nested      The {@link ColumnDateRange} you want to unnest.
	 * @param cteName     The CTE name of the returned {@link QueryStep}.
	 * @return A QueryStep containing an unnested validity date with 1 row per single daterange for each id. For dialects that don't support single column
	 * multiranges, the given predecessor will be returned as is.
	 */
	QueryStep unnestDaterange(ColumnDateRange nested, QueryStep predecessor, String cteName);

	/**
	 * Aggregates the start and end columns of the validity date of entries into one compound string expression.
	 * <p>
	 * Example: {[2013-11-10,2013-11-11),[2015-11-10,2015-11-11)}
	 * <p>
	 * Also, if the aggregated expression contains the dialect specific {@link SqlFunctionProvider#getMaxDateExpression()} or
	 * {@link SqlFunctionProvider#getMinDateExpression()} expression, it should be replaced with the {@link SqlFunctionProvider#INFINITY_SIGN}
	 * or {@link SqlFunctionProvider#MINUS_INFINITY_SIGN}.
	 * <p>
	 * Example: {[-∞,2013-11-11),[2015-11-10,∞)}
	 */
	Field<?> dateRangeAggregation(ColumnDateRange columnDateRange);

	/**
	 * Combines the start and end column of a validity date entry into one compound string expression.
	 * <p>
	 * Example: [2013-11-10,2013-11-11)
	 */
	Field<?> dateRangeToField(ColumnDateRange columnDateRange);

	/**
	 * Calculates the date distance in the given {@link ChronoUnit} between an exclusive end date and an inclusive start date.
	 */
	Field<Integer> dateDistance(ChronoUnit datePart, Field<Date> startDate, Field<Date> endDate);

	Field<Date> addDays(Field<Date> dateColumn, Field<Integer> amountOfDays);

	/**
	 * Return a random aggregated value from the input column.
	 */
	<T> Field<T> random(Field<T> column);

	Condition likeRegex(Field<String> field, String pattern);

	/**
	 * @return The numerical year and quarter of the given date column as "yyyy-Qx" string expression with x being the quarter.
	 */
	Field<String> yearQuarter(Field<Date> dateField);

	default Field<String> stringAggregation(Field<String> stringField, Field<String> delimiter, List<Field<?>> orderByFields) {
		return field(
				"{0}({1}, {2} {3})",
				String.class,
				keyword("string_agg"),
				stringField,
				delimiter,
				orderBy(orderByFields)
		);
	}

	ColumnDateRange allRangeIf(Condition condition);

	/**
	 * Render an array for Conquery processing.
	 */
	default Field<?> arrayOut(List<Field<String>> fields) {
		String concatenated =
				fields.stream()
						// if a field is null, the whole concatenation would be null - but we just want to skip this field in this case,
						// thus concat an empty string
						.map(Field::toString)
						.collect(Collectors.joining(SQL_UNIT_SEPARATOR));
		return field(concatenated, String.class);
	}

	default <T> Field<T> least(List<Field<T>> fields) {
		if (fields.isEmpty()) {
			return null;
		}
		Field<T>[] fieldArray = fields.toArray(Field[]::new);
		// signature only accepts arrays/varargs
		return function("least", fieldArray[0].getType(), fieldArray);
	}

	default <T> Field<T> greatest(List<Field<T>> fields) {
		if (fields.isEmpty()) {
			return null;
		}
		Field<T>[] fieldArray = fields.toArray(Field[]::new);
		// signature only accepts arrays/varargs
		return function("greatest", fieldArray[0].getType(), fieldArray);
	}

	default Condition in(Field<String> column, String[] values) {
		return column.in(values);
	}

	default TableOnConditionStep<Record> innerJoin(Table<?> leftPart, Table<?> rightPart, List<Condition> joinConditions) {
		return leftPart.innerJoin(rightPart).on(joinConditions.toArray(Condition[]::new));
	}

	default TableOnConditionStep<Record> fullOuterJoin(Table<?> leftPart, Table<?> rightPart, List<Condition> joinConditions) {
		return leftPart.fullOuterJoin(rightPart).on(joinConditions.toArray(Condition[]::new));
	}

	default TableOnConditionStep<Record> leftJoin(Table<?> leftPart, Table<?> rightPart, List<Condition> joinConditions) {
		return leftPart.leftJoin(rightPart).on(joinConditions.toArray(Condition[]::new));
	}

	default Field<Date> toDateField(String dateExpression) {
		return toDate(dateExpression, DEFAULT_DATE_FORMAT);
	}

	/**
	 * Empty if start is equal to getMinDateExpression and end is equal to getMaxDateExpression.
	 */
	Condition isNotEmptyDateRange(ColumnDateRange columnDateRange);

	default ColumnDateRange emptyColumnDateRange() {
		return ColumnDateRange.of(inline(null, Date.class), inline(null, Date.class));
	}

	/**
	 * Or-Aggregation of the input field.
	 */
	default Condition orAgg(Field<Boolean> field) {
		return condition(max(field.cast(Integer.class)).gt(0));
	}

	/**
	 * Only necessary to help with Clickhouse because Jooq does not translate nullability constraints into casts.
	 */
	default Field<String> externalId(String id) {
		return inline(id, SQLDataType.VARCHAR);
	}

	/**
	 * Any condition that is acceptable for the specific database on a join.
	 * (e.g. Hana does not like `true`)
	 */
	default Condition unconditionalJoinCondition(){
		return noCondition();
	}

	default Field<Boolean> isNull(Field<?> field){
		return field.isNull();
	}
}