package com.bakdata.conquery.sql.conversion.model;

import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.Getter;
import org.jooq.Field;

@Getter
public class ColumnDateRange implements SqlSelect {

	private static final String DATE_RESTRICTION_COLUMN_NAME = "date_restriction";
	private static final String VALIDITY_DATE_COLUMN_NAME_SUFFIX = "_validity_date";
	private static final String START_SUFFIX = "_start";
	private static final String END_SUFFIX = "_end";

	private final Field<?> range;
	private final Field<Date> start;
	private final Field<Date> end;
	private final String alias;

	protected ColumnDateRange(Field<Date> startColumn, Field<Date> endColumn, String alias) {
		this.range = null;
		this.start = startColumn;
		this.end = endColumn;
		this.alias = alias;
	}

	protected ColumnDateRange(Field<?> range, String alias) {
		this.range = range;
		this.start = null;
		this.end = null;
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

	@Override
	public List<Field<?>> toFields() {
		if (isSingleColumnRange()) {
			return List.of(this.range);
		}
		return Stream.of(this.start, this.end)
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
			return new ColumnDateRange(this.range.as(alias), alias);
		}
		return new ColumnDateRange(
				this.start.as(alias + START_SUFFIX),
				this.end.as(alias + END_SUFFIX),
				alias
		);
	}

}
