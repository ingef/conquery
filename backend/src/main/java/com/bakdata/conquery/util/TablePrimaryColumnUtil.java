package com.bakdata.conquery.util;

import static com.codahale.metrics.MetricRegistry.name;
import static org.jooq.impl.DSL.field;

import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.datasets.Table;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class TablePrimaryColumnUtil {

	public static Field<String> findPrimaryColumn(Table table, DatabaseConfig databaseConfig) {
		String primaryColumnName;
		if (table.getPrimaryColumn() == null) {
			primaryColumnName = databaseConfig.getPrimaryColumn();
		}
		else {
			primaryColumnName = table.getPrimaryColumn().getName();
		}

		return field(name(table.getName(), primaryColumnName), String.class);
	}

}
