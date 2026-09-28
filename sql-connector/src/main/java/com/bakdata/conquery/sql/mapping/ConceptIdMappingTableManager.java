package com.bakdata.conquery.sql.mapping;

import java.util.ArrayList;
import java.util.List;

import org.jooq.CreateTableElementListStep;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertValuesStepN;
import org.jooq.Name;
import org.jooq.RowN;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;

/** Creates and populates the physical lookup table used to resolve concept element IDs. */
public final class ConceptIdMappingTableManager {

	private final DSLContext dslContext;

	public ConceptIdMappingTableManager(DSLContext dslContext) {
		this.dslContext = dslContext;
	}

	public void recreate(ConceptIdMappingTable mapping) {
		delete(mapping.tableName());
		List<Field<?>> fields = createTable(mapping);
		insertMappings(mapping.tableName(), fields, mapping.rows());
		createIndexes(mapping);
	}

	public void delete(Name tableName) {
		try {
			dslContext.dropTable(tableName).execute();
		}
		catch (DataAccessException ignored) {
			// Some supported databases do not provide a portable DROP TABLE IF EXISTS variant.
		}
	}

	private List<Field<?>> createTable(ConceptIdMappingTable mapping) {
		List<Field<?>> fields = mapping.tableFields();
		CreateTableElementListStep createTable = dslContext.createTable(mapping.tableName()).columns(fields);
		createTable.execute();
		return fields;
	}

	private void insertMappings(Name tableName, List<Field<?>> fields, List<RowN> rows) {
		List<InsertValuesStepN<?>> inserts = new ArrayList<>(rows.size());
		for (RowN row : rows) {
			inserts.add(dslContext.insertInto(mappingTable(tableName)).columns(fields).values(row));
		}
		dslContext.batch(inserts).execute();
	}

	private void createIndexes(ConceptIdMappingTable mapping) {
		if (dslContext.dialect().family() == SQLDialect.CLICKHOUSE) {
			return;
		}
		String indexToken = Integer.toUnsignedString(mapping.tableName().hashCode(), 16);
		if (!mapping.keyFields().isEmpty()) {
			dslContext.createIndex(org.jooq.impl.DSL.name("cq_map_%s_keys".formatted(indexToken)))
					.on(mapping.table(), mapping.keyFields().stream().map(Field::sortDefault).toList())
					.execute();
		}
		dslContext.createIndex(org.jooq.impl.DSL.name("cq_map_%s_id".formatted(indexToken)))
				.on(mapping.table(), mapping.resolvedId().sortDefault())
				.execute();
	}

	private static org.jooq.Table<org.jooq.Record> mappingTable(Name tableName) {
		return org.jooq.impl.DSL.table(tableName);
	}
}
