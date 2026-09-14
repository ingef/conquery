package com.bakdata.conquery.sql.compiler.ir;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.sql.model.schema.EntitySchema;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

/** Creates SQL identifiers for a resolved entity schema. */
final class EntitySchemaSql {

	private EntitySchemaSql() {
	}

	static Table<Record> table(EntitySchema entitySchema) {
		return DSL.table(tableName(entitySchema));
	}

	static Name tableName(EntitySchema entitySchema) {
		return name(entitySchema.table().physicalName().toArray(String[]::new));
	}

	static Field<String> primaryId(EntitySchema entitySchema) {
		List<String> qualifiedName = new ArrayList<>(entitySchema.table().physicalName());
		qualifiedName.add(entitySchema.primaryId().physicalName());
		return field(name(qualifiedName.toArray(String[]::new)), String.class);
	}
}
