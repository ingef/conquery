package com.bakdata.conquery.sql.translate.ir;

import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import lombok.experimental.UtilityClass;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

/** Creates SQL identifiers for resolved physical schema objects. */
@UtilityClass
public final class SchemaSql {

	public static Table<Record> table(SqlTable table) {
		return DSL.table(tableName(table));
	}

	public static Name tableName(SqlTable table) {
		return table.physicalName();
	}

	public static <T> Field<T> field(ResolvedColumn column, Class<T> type) {
		return DSL.field(columnName(column), type);
	}

	public static Name columnName(ResolvedColumn column) {
		return column.table().physicalName().append(column.physicalName());
	}
}
