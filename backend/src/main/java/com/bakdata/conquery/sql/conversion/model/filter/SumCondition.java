package com.bakdata.conquery.sql.conversion.model.filter;

import javax.annotation.Nullable;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.Column;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class SumCondition extends RangeCondition {

	public SumCondition(Field<? extends Number> column, IRange<? extends Number, ?> range) {
		super(column, range);
	}

	public static SumCondition onColumn(Column column, @Nullable Column subtractColumn, IRange<? extends Number, ?> range) {

		String tableName = column.getTable().getName();
		String columnName = column.getName();
		Field<Number> field = DSL.field(DSL.name(tableName, columnName), Number.class);

		if (subtractColumn == null) {
			return new SumCondition(field, range);
		}

		String subtractColumnName = subtractColumn.getName();
		String subtractTableName = subtractColumn.getTable().getName();
		Field<Number> subtractField = DSL.field(DSL.name(subtractTableName, subtractColumnName), Number.class);
		return new SumCondition(field.minus(subtractField), range);
	}

	@Override
	public ConditionType type() {
		return ConditionType.GROUP;
	}

}
