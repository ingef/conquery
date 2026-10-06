package com.bakdata.conquery.sql.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.RowN;
import org.jooq.Table;
import org.jooq.impl.DSL;

/** Resolved metadata and lazily materialized contents of a concept ID lookup table. */
public final class ConceptIdMappingTable {

	public static final String RESOLVED_ID_COLUMN = "resolved_id";

	private final Name tableName;
	private final List<Field<?>> keyFields;
	private Supplier<List<RowN>> rowsSupplier;
	private List<RowN> rows;

	public ConceptIdMappingTable(Name tableName, List<Field<?>> keyFields, List<RowN> rows) {
		this.tableName = Objects.requireNonNull(tableName, "tableName");
		this.keyFields = List.copyOf(keyFields);
		this.rows = List.copyOf(rows);
	}

	ConceptIdMappingTable(Name tableName, List<Field<?>> keyFields, Supplier<List<RowN>> rowsSupplier) {
		this.tableName = Objects.requireNonNull(tableName, "tableName");
		this.keyFields = List.copyOf(keyFields);
		this.rowsSupplier = Objects.requireNonNull(rowsSupplier, "rowsSupplier");
	}

	public Name tableName() {
		return tableName;
	}

	public List<Field<?>> keyFields() {
		return keyFields;
	}

	public synchronized List<RowN> rows() {
		if (rows == null) {
			rows = List.copyOf(rowsSupplier.get());
			rowsSupplier = null;
		}
		return rows;
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
