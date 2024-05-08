package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.Column;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class CountCondition extends RangeCondition {

	public CountCondition(Field<?> column, IRange<? extends Number, ?> range) {
		super(column, range);
	}

	public static CountCondition onColumn(Column column, IRange<? extends Number, ?> range) {
		String tableName = column.getTable().getName();
		String columnName = column.getName();
		Field<Number> field = DSL.field(DSL.name(tableName, columnName), Number.class);
		return new CountCondition(field, range);
	}

	@Override
	public ConditionType type() {
		return ConditionType.GROUP;
	}

}
