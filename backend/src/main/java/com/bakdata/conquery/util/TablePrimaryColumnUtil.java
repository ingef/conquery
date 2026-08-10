package com.bakdata.conquery.util;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;

import com.bakdata.conquery.models.datasets.Table;
import org.jooq.Field;

import java.util.Objects;

public class TablePrimaryColumnUtil {

	public static Field<String> findPrimaryColumn(Table table, String defaultPrimaryColumn) {
		String primaryColumnName = defaultPrimaryColumn;

		if (table.getPrimaryColumn() != null) {
			primaryColumnName = table.getPrimaryColumn().getName();
		}

		if (primaryColumnName == null) {
			throw new IllegalArgumentException("Unable to determine primary column for table " + table.getId());
		}

		return field(name(table.getName(), primaryColumnName), String.class);
	}

}
