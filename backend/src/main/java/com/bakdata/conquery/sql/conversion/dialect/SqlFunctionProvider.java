package com.bakdata.conquery.sql.conversion.dialect;

import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import org.jooq.Condition;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Name;
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
	 * A date restriction condition is true if holds: dateRestrictionStart <= validityDateEnd and dateRestrictionEnd >= validityDateStart
	 */
	Condition dateRestriction(ColumnDateRange dateRestrictionRange, ColumnDateRange validityFieldRange);

	ColumnDateRange daterange(CDateRange dateRestriction);

	ColumnDateRange daterange(ValidityDate validityDate, String qualifier, String conceptLabel);

	ColumnDateRange aggregated(ColumnDateRange columnDateRange);

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
	Field<String> validityDateStringAggregation(ColumnDateRange columnDateRange);

	Field<Integer> dateDistance(ChronoUnit datePart, Name startDateColumn, Date endDateExpression);

	Field<Date> addDays(Field<Date> dateColumn, int amountOfDays);

	<T> Field<T> first(Field<T>  field, List<Field<?>> orderByColumn);

	<T> Field<T> last(Field<T> column, List<Field<?>> orderByColumns);

	<T> Field<T> random(Field<T> column);

	Condition likeRegex(Field<String> field, String pattern);

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
			Field<Object> leftPartPrimaryColumn,
			Field<Object> rightPartPrimaryColumn
	) {
		return leftPartQueryBase
				.innerJoin(DSL.name(rightPartQS.getCteName()))
				.on(leftPartPrimaryColumn.eq(rightPartPrimaryColumn));
	}

	default TableOnConditionStep<Record> fullOuterJoin(
			Table<Record> leftPartQueryBase,
			QueryStep rightPartQS,
			Field<Object> leftPartPrimaryColumn,
			Field<Object> rightPartPrimaryColumn
	) {
		return leftPartQueryBase
				.fullOuterJoin(DSL.name(rightPartQS.getCteName()))
				.on(leftPartPrimaryColumn.eq(rightPartPrimaryColumn));
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
