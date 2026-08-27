package com.bakdata.conquery.sql.conversion.model;

import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.Getter;
import lombok.ToString;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

// TODO split this class up into Dialect specific versions.
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class ColumnDateRange implements SqlSelect {

	private static final String VALIDITY_DATE_COLUMN_NAME_SUFFIX = "_validity_date";
	private static final String START_SUFFIX = "_start";
	private static final String END_SUFFIX = "_end";

	@ToString.Include
	private final Field<Date> start;
	@ToString.Include
	private final Field<Date> end;
	private final String alias;

	protected ColumnDateRange(Field<Date> startColumn, Field<Date> endColumn, String alias) {
		start = startColumn;
		end = endColumn;
		this.alias = alias;
	}

	public static ColumnDateRange of(Field<Date> startColumn, Field<Date> endColumn) {
		return new ColumnDateRange(startColumn, endColumn, "");
	}

	public static ColumnDateRange of(Field<Date> startColumn, Field<Date> endColumn, String alias) {
		return new ColumnDateRange(startColumn, endColumn, alias);
	}


	public ColumnDateRange asValidityDateRange(String alias) {
		return as(alias + VALIDITY_DATE_COLUMN_NAME_SUFFIX);
	}

	@Override
	public List<Field<?>> toFields() {
		return Stream.of(start, end).collect(Collectors.toList());
	}

	@Override
	public ColumnDateRange qualify(String qualifier) {
		return new ColumnDateRange(
			QualifyingUtil.qualify(getStart(), qualifier),
			QualifyingUtil.qualify(getEnd(), qualifier),
			getAlias()
		);
	}

	@Override
	public List<String> requiredColumns() {
		return toFields().stream().map(Field::getName).distinct().toList();
	}

	public ColumnDateRange as(String alias) {
		return new ColumnDateRange(
			start.as(alias + START_SUFFIX),
			end.as(alias + END_SUFFIX),
			alias
		);
	}

	public ColumnDateRange coalesce(ColumnDateRange right) {
		return ColumnDateRange.of(
			DSL.coalesce(start, right.getStart()),
			DSL.coalesce(end, right.getEnd())
		).as(alias);
	}

	public Condition join(ColumnDateRange right) {
		return start.eq(right.getStart()).and(end.eq(right.getEnd()));
	}

	public Condition isNotNull() {
		return start.isNotNull().and(end.isNotNull());
	}

	public static Condition isNotEmpty(ColumnDateRange columnDateRange) {
		return columnDateRange.getStart().isNotNull().and(columnDateRange.getEnd().isNotNull());
	}

}
