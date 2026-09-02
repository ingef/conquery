package com.bakdata.conquery.quarkus.ids;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record ColumnId(TableId tableId, String name) {

	public ColumnId {
		if (tableId == null) {
			throw new IllegalArgumentException("Column id table must not be null.");
		}
		name = IdPart.requireValid(name, "Column id name");
	}

	public DatasetId datasetId() {
		return tableId.datasetId();
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static ColumnId parse(String value) {
		String[] parts = IdPart.split(value, "Column id", 3);
		if (parts.length != 3) {
			throw new IllegalArgumentException("Column id must have exactly three parts: " + value);
		}
		return new ColumnId(new TableId(DatasetId.parse(parts[0]), parts[1]), parts[2]);
	}

	public static ColumnId valueOf(String value) {
		return parse(value);
	}

	@Override
	@JsonValue
	public String toString() {
		return tableId + "." + name;
	}
}
