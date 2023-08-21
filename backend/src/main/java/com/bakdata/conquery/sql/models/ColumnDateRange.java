package com.bakdata.conquery.sql.models;

import java.util.List;

import lombok.Getter;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Getter
public class ColumnDateRange {

	private static final String DATE_RESTRICTION_COLUMN_NAME = "date_restriction";
	private static final String VALIDITY_DATE_COLUMN_NAME_SUFFIX = "_validity_date";
	private static final String START_SUFFIX = "_start";
	private static final String END_SUFFIX = "_end";

	private final boolean isEmpty;
	private final Field<Object> range;
	private final Field<Object> start;
	private final Field<Object> end;

	private ColumnDateRange(boolean isEmpty, Field<Object> range, Field<Object> startColumn, Field<Object> endColumn) {
		this.isEmpty = isEmpty;
		this.range = range;
		this.start = startColumn;
		this.end = endColumn;
	}

	public static ColumnDateRange of(Field<Object> rangeColumn) {
		return new ColumnDateRange(false, rangeColumn, null, null);
	}

	public static ColumnDateRange of(Field<Object> startColumn, Field<Object> endColumn) {
		return new ColumnDateRange(true, null, startColumn, endColumn);
	}

	public ColumnDateRange asDateRestrictionRange() {
		return this.as(DATE_RESTRICTION_COLUMN_NAME);
	}

	public ColumnDateRange asValidityDateRange(String alias) {
		return this.as(alias + VALIDITY_DATE_COLUMN_NAME_SUFFIX);
	}

	/**
	 * @return True if this {@link ColumnDateRange} consists of only 1 column.
	 * False if it consists of a start and end field.
	 */
	public boolean isSingleColumnRange() {
		return this.range != null;
	}

	public List<Field<Object>> toFields() {
		if (isSingleColumnRange()) {
			return List.of(this.range);
		}
		return List.of(this.start, this.end);
	}

	public ColumnDateRange qualify(String qualifier) {
		if (isSingleColumnRange()) {
			return ColumnDateRange.of(mapFieldOntoQualifier(getRange(), qualifier));
		}
		return ColumnDateRange.of(
				mapFieldOntoQualifier(getStart(), qualifier),
				mapFieldOntoQualifier(getEnd(), qualifier)
		);
	}

	private ColumnDateRange as(String alias) {
		if (isSingleColumnRange()) {
			return ColumnDateRange.of(this.range.as(alias));
		}
		return ColumnDateRange.of(
				this.start.as(alias + START_SUFFIX),
				this.end.as(alias + END_SUFFIX)
		);
	}

	private Field<Object> mapFieldOntoQualifier(Field<Object> field, String qualifier) {
		return DSL.field(DSL.name(qualifier, field.getName()));
	}

}
