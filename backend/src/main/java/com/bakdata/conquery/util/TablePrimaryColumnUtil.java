package com.bakdata.conquery.util;

import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.datasets.Table;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class TablePrimaryColumnUtil {

	public static Field<Object> findPrimaryColumn(Table table, DatabaseConfig databaseConfig) {
		String primaryColumnName = table.getPrimaryColumn() == null
								   ? databaseConfig.getPrimaryColumn()
								   : table.getPrimaryColumn().getName();
		return DSL.field(DSL.name(table.getName(), primaryColumnName));
	}

}
