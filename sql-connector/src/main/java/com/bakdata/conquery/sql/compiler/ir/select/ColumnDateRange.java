package com.bakdata.conquery.sql.compiler.ir.select;

import java.sql.Date;
import java.util.List;

import com.bakdata.conquery.sql.compiler.ir.QualifyingUtil;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

/** A two-column date-range representation used by compiler transformations. */
public final class ColumnDateRange implements SqlSelect {

	private static final String VALIDITY_DATE_COLUMN_NAME_SUFFIX = "_validity_date";
	private static final String START_SUFFIX = "_start";
	private static final String END_SUFFIX = "_end";

	private final Field<Date> start;
	private final Field<Date> end;
	private final String alias;

	private ColumnDateRange(Field<Date> start, Field<Date> end, String alias) {
		this.start = start;
		this.end = end;
		this.alias = alias;
	}

	public static ColumnDateRange of(Field<Date> start, Field<Date> end) {
		return new ColumnDateRange(start, end, "");
	}

	public static ColumnDateRange of(Field<Date> start, Field<Date> end, String alias) {
		return new ColumnDateRange(start, end, alias);
	}

	public Field<Date> getStart() {
		return start;
	}

	public Field<Date> getEnd() {
		return end;
	}

	public String getAlias() {
		return alias;
	}

	public ColumnDateRange asValidityDateRange(String alias) {
		return as(alias + VALIDITY_DATE_COLUMN_NAME_SUFFIX);
	}

	@Override
	public List<Field<?>> toFields() {
		return List.of(start, end);
	}

	@Override
	public ColumnDateRange qualify(String qualifier) {
		return new ColumnDateRange(
				QualifyingUtil.qualify(start, qualifier),
				QualifyingUtil.qualify(end, qualifier),
				alias
		);
	}

	@Override
	public List<String> requiredColumns() {
		return toFields().stream()
				.map(Field::getName)
				.distinct()
				.toList();
	}

	public ColumnDateRange as(String alias) {
		return new ColumnDateRange(start.as(alias + START_SUFFIX), end.as(alias + END_SUFFIX), alias);
	}

	public ColumnDateRange coalesce(ColumnDateRange right) {
		return ColumnDateRange.of(DSL.coalesce(start, right.start), DSL.coalesce(end, right.end)).as(alias);
	}

	public Condition join(ColumnDateRange right) {
		return start.eq(right.start).and(end.eq(right.end));
	}

	public Condition isNotNull() {
		return start.isNotNull().and(end.isNotNull());
	}

	public static Condition isNotEmpty(ColumnDateRange columnDateRange) {
		return columnDateRange.start.isNotNull().and(columnDateRange.end.isNotNull());
	}

	@Override
	public String toString() {
		return "ColumnDateRange(start=%s, end=%s)".formatted(start, end);
	}
}
