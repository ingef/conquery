package com.bakdata.conquery.util;

import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.models.datasets.Table;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class TablePrimaryColumnUtil {

	public static Field<Object> findPrimaryColumn(Table table, SqlConnectorConfig sqlConfig) {
		String primaryColumnName = table.getPrimaryColum() == null
								   ? sqlConfig.getPrimaryColumn()
								   : table.getPrimaryColum().getName();
		return DSL.field(DSL.name(table.getName(), primaryColumnName));
	}

}
