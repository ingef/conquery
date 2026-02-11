package com.bakdata.conquery.util;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;

import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.datasets.Table;
import org.jooq.Field;

public class TablePrimaryColumnUtil {

	public static Field<String> findPrimaryColumn(Table table, DatabaseConfig databaseConfig) {
		String primaryColumnName;
		if (table.getPrimaryColumn() != null) {
			primaryColumnName = table.getPrimaryColumn().getName();
		}
		else {
			primaryColumnName = databaseConfig.getPrimaryColumn();
		}

		return field(name(table.getName(), primaryColumnName), String.class);
	}

}
