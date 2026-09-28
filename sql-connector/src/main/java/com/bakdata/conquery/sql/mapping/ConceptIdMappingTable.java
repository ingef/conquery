package com.bakdata.conquery.sql.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.RowN;
import org.jooq.Table;
import org.jooq.impl.DSL;

/** Resolved physical contents of a concept ID lookup table. */
public record ConceptIdMappingTable(
		Name tableName,
		List<Field<?>> keyFields,
		List<RowN> rows
) {

	public static final String RESOLVED_ID_COLUMN = "resolved_id";

	public ConceptIdMappingTable {
		Objects.requireNonNull(tableName, "tableName");
		keyFields = List.copyOf(keyFields);
		rows = List.copyOf(rows);
	}

	public Table<Record> table() {
		return DSL.table(tableName);
	}

	public Field<Integer> resolvedId() {
		return DSL.field(DSL.name(tableName, DSL.name(RESOLVED_ID_COLUMN)), Integer.class);
	}

	public Field<?> mappingField(Field<?> keyField) {
		return DSL.field(DSL.name(tableName, keyField.getUnqualifiedName()), keyField.getDataType());
	}

	public List<Field<?>> tableFields() {
		List<Field<?>> fields = new ArrayList<>(keyFields.size() + 1);
		fields.add(DSL.field(DSL.name(RESOLVED_ID_COLUMN), Integer.class));
		fields.addAll(keyFields);
		return List.copyOf(fields);
	}
}
