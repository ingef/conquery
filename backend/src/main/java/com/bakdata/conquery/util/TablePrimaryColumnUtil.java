package com.bakdata.conquery.util;

import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.datasets.Table;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Slf4j
public class TablePrimaryColumnUtil {

	public static Field<Object> findPrimaryColumn(Table table, DatabaseConfig databaseConfig) {
		String primaryColumnName = table.getPrimaryColumn() == null
								   ? databaseConfig.getPrimaryColumn()
								   : table.getPrimaryColumn().getName();
		return DSL.field(DSL.name(table.getName(), primaryColumnName));
	}

}
