package com.bakdata.conquery.sql.compiler.ir;

import static org.jooq.impl.DSL.name;

import java.util.ArrayList;
import java.util.List;

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
		return name(table.physicalName().toArray(String[]::new));
	}

	public static <T> Field<T> field(ResolvedColumn column, Class<T> type) {
		return DSL.field(columnName(column), type);
	}

	public static Name columnName(ResolvedColumn column) {
		List<String> qualifiedName = new ArrayList<>(column.table().physicalName());
		qualifiedName.add(column.physicalName());
		return name(qualifiedName.toArray(String[]::new));
	}
}
