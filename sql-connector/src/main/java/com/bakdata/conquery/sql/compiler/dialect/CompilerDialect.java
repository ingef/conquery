package com.bakdata.conquery.sql.compiler.dialect;

import java.sql.Date;
import java.util.List;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.model.range.DateRange;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

/**
 * Database-specific capabilities exposed to the framework-neutral SQL compiler.
 *
 * <p>This contract belongs to the SQL connector and must only use types owned by the connector or its public model
 * dependencies. Backend query DTOs, converter registries, runtime connections, and result processing are deliberately
 * excluded.</p>
 *
 * <p>Concrete backend integrations may extend this contract with temporary adapter interfaces while the legacy compiler
 * is migrated. Those adapters must not become dependencies of the framework-neutral compiler.</p>
 */
public interface CompilerDialect {

	/** Expression used as the database-specific lower infinity sentinel for dates. */
	Field<Date> minimumDate();

	/** Expression used as the database-specific upper infinity sentinel for dates. */
	Field<Date> maximumDate();

	/** Date range representing no date value. */
	default ColumnDateRange emptyDateRange() {
		return ColumnDateRange.of(DSL.inline(null, Date.class), DSL.inline(null, Date.class));
	}

	/** Logically unbounded date range using the database-specific date sentinels. */
	default ColumnDateRange unboundedDateRange() {
		return ColumnDateRange.of(minimumDate(), maximumDate());
	}

	/**
	 * Logically unbounded date range when the supplied condition holds, and an empty range otherwise.
	 */
	default ColumnDateRange conditionalUnboundedDateRange(Condition condition) {
		return ColumnDateRange.of(
				DSL.when(condition.isTrue(), minimumDate()),
				DSL.when(condition.isTrue(), maximumDate())
		);
	}

	/** Regex fragment matching any number of characters in this dialect. */
	default String regexAnyCharacters() {
		return ".*";
	}

	/** Test whether a string field matches the supplied regular expression. */
	default Condition regexMatches(Field<String> field, String pattern) {
		return field.likeRegex(pattern);
	}

	/** SQL expression for an already-resolved external entity ID. */
	default Field<String> externalId(String id) {
		return DSL.inline(id, SQLDataType.VARCHAR);
	}

	/**
	 * Render ordered external string values in the physical representation expected by this dialect's result reader.
	 */
	default Field<?> externalStringValues(List<String> values) {
		if (values.isEmpty()) {
			return DSL.inline(null, String.class);
		}
		Field<String> concatenated = DSL.inline(values.getFirst());
		for (String value : values.subList(1, values.size())) {
			concatenated = DSL.concat(concatenated, DSL.inline(String.valueOf((char) 31)), DSL.inline(value));
		}
		return concatenated;
	}

	/** Dummy table used by dialects that cannot select literal values without a FROM clause. */
	default Table<? extends Record> literalSelectTable() {
		return DSL.noTable();
	}

	/** Convert an inclusive resolved date range to the compiler's half-open date-range representation. */
	default ColumnDateRange dateRangeLiteral(DateRange dateRange) {
		Field<Date> start = dateRange.startInclusive()
				.<Field<Date>>map(value -> DSL.inline(Date.valueOf(value)))
				.orElseGet(this::minimumDate);
		Field<Date> end = dateRange.endInclusive()
				.<Field<Date>>map(value -> DSL.inline(Date.valueOf(value.plusDays(1))))
				.orElseGet(this::maximumDate);
		return ColumnDateRange.of(start, end);
	}

	/**
	 * Aggregate a field to an arbitrary value from its group.
	 *
	 * <p>The exact expression is dialect-specific. For example, a database without an {@code ANY_VALUE} aggregate may
	 * use {@code MIN} as an equivalent deterministic representative where the compiler only requires one value.</p>
	 */
	<T> Field<T> anyValue(Field<T> field);

	/**
	 * Render the bounds of one date range into the database-specific physical result representation.
	 */
	Field<?> renderDateRange(Field<Date> start, Field<Date> end);

	/**
	 * Aggregate multiple date ranges into the database-specific physical result representation.
	 */
	Field<?> aggregateDateRanges(Field<Date> start, Field<Date> end);

	/** Maximum identifier length supported by the target database. */
	int getNameMaxLength();

	/** Whether the dialect represents date ranges in one database column instead of separate start and end columns. */
	default boolean supportsSingleColumnRanges() {
		return false;
	}

	/**
	 * Expand a physical single-column date range into a query step exposing its logical start and end fields.
	 *
	 * @throws UnsupportedOperationException when a dialect advertises single-column ranges without implementing the
	 * expansion
	 */
	default QueryStep unnestDateRange(ColumnDateRange dateRange, QueryStep predecessor, String cteName) {
		throw new UnsupportedOperationException("Single-column date-range expansion is not implemented by this dialect");
	}
}
