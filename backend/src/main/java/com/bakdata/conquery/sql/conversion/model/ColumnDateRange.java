package com.bakdata.conquery.sql.conversion.model;

import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.Getter;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Getter
public class ColumnDateRange implements SqlSelect {

	private static final String VALIDITY_DATE_COLUMN_NAME_SUFFIX = "_validity_date";
	private static final String START_SUFFIX = "_start";
	private static final String END_SUFFIX = "_end";

	private final Field<?> range;
	private final Field<Date> start;
	private final Field<Date> end;
	private final String alias;

	protected ColumnDateRange(Field<Date> startColumn, Field<Date> endColumn, String alias) {
		range = null;
		start = startColumn;
		end = endColumn;
		this.alias = alias;
	}

	protected ColumnDateRange(Field<?> range, String alias) {
		this.range = range;
		start = null;
		end = null;
		this.alias = alias;
	}

	public static ColumnDateRange of(Field<?> rangeColumn, String alias) {
		return new ColumnDateRange(rangeColumn, alias);
	}

	public static ColumnDateRange of(Field<?> rangeColumn) {
		return new ColumnDateRange(rangeColumn, "");
	}

	public static ColumnDateRange of(Field<Date> startColumn, Field<Date> endColumn) {
		return new ColumnDateRange(startColumn, endColumn, "");
	}

	public static ColumnDateRange of(Field<Date> startColumn, Field<Date> endColumn, String alias) {
		return new ColumnDateRange(startColumn, endColumn, alias);
	}

	public static ColumnDateRange empty() {
		final Field<String> emptyRange = DSL.field(DSL.val("{}"));
		return ColumnDateRange.of(emptyRange);
	}

	public ColumnDateRange asValidityDateRange(String alias) {
		return as(alias + VALIDITY_DATE_COLUMN_NAME_SUFFIX);
	}

	/**
	 * @return True if this {@link ColumnDateRange} consists of only 1 column.
	 * False if it consists of a start and end field.
	 */
	public boolean isSingleColumnRange() {
		return range != null;
	}

	@Override
	public List<Field<?>> toFields() {
		if (isSingleColumnRange()) {
			return List.of(range);
		}
		return Stream.of(start, end)
					 .collect(Collectors.toList());
	}

	@Override
	public ColumnDateRange qualify(String qualifier) {
		if (isSingleColumnRange()) {
			return new ColumnDateRange(QualifyingUtil.qualify(getRange(), qualifier), getAlias());
		}
		return new ColumnDateRange(
				QualifyingUtil.qualify(getStart(), qualifier),
				QualifyingUtil.qualify(getEnd(), qualifier),
				getAlias()
		);
	}

	@Override
	public List<String> requiredColumns() {
		return toFields().stream().map(Field::getName).toList();
	}

	public ColumnDateRange as(String alias) {
		if (isSingleColumnRange()) {
			return new ColumnDateRange(range.as(alias), alias);
		}
		return new ColumnDateRange(
				start.as(alias + START_SUFFIX),
				end.as(alias + END_SUFFIX),
				alias
		);
	}

	public ColumnDateRange coalesce(ColumnDateRange right) {
		if (isSingleColumnRange() != right.isSingleColumnRange()) {
			throw new UnsupportedOperationException("Can only join ColumnDateRanges of same type");
		}
		if (isSingleColumnRange()) {
			return ColumnDateRange.of(DSL.coalesce(range, right.getRange())).as(alias);
		}
		return ColumnDateRange.of(
				DSL.coalesce(start, right.getStart()),
				DSL.coalesce(end, right.getEnd())
		).as(alias);
	}

	public Condition join(ColumnDateRange right) {
		if (isSingleColumnRange() != right.isSingleColumnRange()) {
			throw new UnsupportedOperationException("Can only join ColumnDateRanges of same type");
		}
		if (isSingleColumnRange()) {
			return range.coerce(Object.class).eq(right.getRange());
		}
		return start.eq(right.getStart()).and(end.eq(right.getEnd()));
	}

	public Condition isNotNull() {
		if (isSingleColumnRange()) {
			return range.isNotNull();
		}
		return start.isNotNull().and(end.isNotNull());
	}

}
