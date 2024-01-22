package com.bakdata.conquery.sql.conversion.model;

import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	private final Field<?> range;
	private final Field<Date> start;
	private final Field<Date> end;

	private ColumnDateRange(boolean isEmpty, Field<?> range, Field<Date> startColumn, Field<Date> endColumn) {
		this.isEmpty = isEmpty;
		this.range = range;
		this.start = startColumn;
		this.end = endColumn;
	}

	public static ColumnDateRange of(Field<?> rangeColumn) {
		return new ColumnDateRange(false, rangeColumn, null, null);
	}

	public static ColumnDateRange of(Field<Date> startColumn, Field<Date> endColumn) {
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

	public List<Field<?>> toFields() {
		if (isSingleColumnRange()) {
			return List.of(this.range);
		}
		return Stream.of(this.start, this.end)
					 .collect(Collectors.toList());
	}

	public ColumnDateRange qualify(String qualifier) {
		if (isSingleColumnRange()) {
			return ColumnDateRange.of(mapFieldOntoQualifier(getRange(), Object.class, qualifier));
		}
		return ColumnDateRange.of(
				mapFieldOntoQualifier(getStart(), Date.class, qualifier),
				mapFieldOntoQualifier(getEnd(), Date.class, qualifier)
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

	private <Z> Field<Z> mapFieldOntoQualifier(Field<?> field, Class<Z> fieldType, String qualifier) {
		return DSL.field(DSL.name(qualifier, field.getName()), fieldType);
	}

}
