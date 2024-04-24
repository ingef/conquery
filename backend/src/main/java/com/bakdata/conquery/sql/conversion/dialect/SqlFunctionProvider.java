package com.bakdata.conquery.sql.conversion.dialect;

import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import org.jooq.Condition;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableOnConditionStep;
import org.jooq.impl.DSL;

/**
 * Provider of SQL functions.
 */
public interface SqlFunctionProvider {

	String DEFAULT_DATE_FORMAT = "yyyy-mm-dd";
	String INFINITY_SIGN = "∞";
	String MINUS_INFINITY_SIGN = "-∞";

	String getMinDateExpression();

	String getMaxDateExpression();

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
	Table<? extends Record> getNoOpTable();

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
	List<ColumnDateRange> forCDateSet(CDateSet dateset, SharedAliases alias);

	/**
	 * Creates a {@link ColumnDateRange} for a tables {@link CQTable}s validity date.
	 */
	ColumnDateRange forTablesValidityDate(CQTable cqTable, String alias);

	/**
	 * Creates a {@link ColumnDateRange} for a tables {@link CQTable}s validity date. The validity dates bounds will be restricted by the given date
	 * restriction.
	 */
	ColumnDateRange forTablesValidityDate(CQTable cqTable, CDateRange dateRestriction, String alias);

	ColumnDateRange aggregated(ColumnDateRange columnDateRange);

	/**
	 * Given a single-column {@link ColumnDateRange}, it will create a new {@link ColumnDateRange} with a start and end field.
	 * For dialects that don't support single-column ranges, it will create a copy of the given {@link ColumnDateRange}.
	 *
	 * @return A {@link ColumnDateRange} which has a start and end field.
	 */
	ColumnDateRange toDualColumn(ColumnDateRange columnDateRange);

	ColumnDateRange intersection(ColumnDateRange left, ColumnDateRange right);

	/**
	 * @param predecessor The predeceasing step containing an aggregated validity date.
	 * @return A QueryStep containing an unnested validity date with 1 row per single daterange for each id. For dialects that don't support single column
	 * multiranges, the given predecessor will be returned as is.
	 */
	QueryStep unnestValidityDate(QueryStep predecessor, SqlTables sqlTables);

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
	Field<String> daterangeStringAggregation(ColumnDateRange columnDateRange);

	/**
	 * Combines the start and end column of a validity date entry into one compound string expression.
	 * <p>
	 * Example: [2013-11-10,2013-11-11)
	 */
	Field<String> daterangeStringExpression(ColumnDateRange columnDateRange);

	Field<Integer> dateDistance(ChronoUnit datePart, Field<Date> startDate, Field<Date> endDate);

	Field<Date> addDays(Field<Date> dateColumn, Field<Integer> amountOfDays);

	<T> Field<T> first(Field<T>  field, List<Field<?>> orderByColumn);

	<T> Field<T> last(Field<T> column, List<Field<?>> orderByColumns);

	<T> Field<T> random(Field<T> column);

	Condition likeRegex(Field<String> field, String pattern);

	/**
	 * @return The numerical year and quarter of the given date column as "yyyy-Qx" string expression with x being the quarter.
	 */
	Field<String> yearQuarter(Field<Date> dateField);

	Field<Object[]> asArray(List<Field<?>> fields);

	default <T> Field<T> least(List<Field<T>> fields) {
		if (fields.isEmpty()) {
			return null;
		}
		Field<T>[] fieldArray = fields.toArray(Field[]::new);
		// signature only accepts arrays/varargs
		return DSL.function("least", fieldArray[0].getType(), fieldArray);
	}

	default <T> Field<T> greatest(List<Field<T>> fields) {
		if (fields.isEmpty()) {
			return null;
		}
		Field<T>[] fieldArray = fields.toArray(Field[]::new);
		// signature only accepts arrays/varargs
		return DSL.function("greatest", fieldArray[0].getType(), fieldArray);
	}

	default Condition in(Field<String> column, String[] values) {
		return column.in(values);
	}

	default TableOnConditionStep<Record> innerJoin(
			Table<Record> leftPartQueryBase,
			QueryStep rightPartQS,
			List<Condition> joinConditions
	) {
		return leftPartQueryBase
				.innerJoin(DSL.name(rightPartQS.getCteName()))
				.on(joinConditions.toArray(Condition[]::new));
	}

	default TableOnConditionStep<Record> fullOuterJoin(
			Table<Record> leftPartQueryBase,
			QueryStep rightPartQS,
			List<Condition> joinConditions
	) {
		return leftPartQueryBase
				.fullOuterJoin(DSL.name(rightPartQS.getCteName()))
				.on(joinConditions.toArray(Condition[]::new));
	}

	default TableOnConditionStep<Record> leftJoin(
			Table<Record> leftPartQueryBase,
			QueryStep rightPartQS,
			List<Condition> joinConditions
	) {
		return leftPartQueryBase
				.leftJoin(DSL.name(rightPartQS.getCteName()))
				.on(joinConditions.toArray(Condition[]::new));
	}

	default Field<Date> toDateField(String dateExpression) {
		return DSL.toDate(dateExpression, DEFAULT_DATE_FORMAT);
	}

	default Field<String> replace(Field<String> target, String old, String _new) {
		return DSL.function("replace", String.class, target, DSL.val(old), DSL.val(_new));
	}

	default Field<String> prefixStringAggregation(Field<String> field, String prefix) {
		return DSL.field(
				"'[' || {0}({1}, {2}) || ']'",
				String.class,
				DSL.keyword("STRING_AGG"),
				DSL.when(field.like(DSL.inline(prefix + "%")), field),
				DSL.val(", ")
		);
	}

}
