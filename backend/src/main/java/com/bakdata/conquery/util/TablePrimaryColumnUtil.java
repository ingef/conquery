package com.bakdata.conquery.util;

import com.bakdata.conquery.models.datasets.Table;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class TablePrimaryColumnUtil {

	public static Field<String> findPrimaryColumn(Table table, String defaultPrimaryColumn) {
		String primaryColumnName = table.getPrimaryColumn() == null
								   ? defaultPrimaryColumn
								   : table.getPrimaryColumn().getName();
		return DSL.field(DSL.name(table.getName(), primaryColumnName), String.class);
	}

}
