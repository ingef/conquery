package com.bakdata.conquery.quarkus.ids;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record TableId(DatasetId datasetId, String name) {

	public TableId {
		if (datasetId == null) {
			throw new IllegalArgumentException("Table id dataset must not be null.");
		}
		name = IdPart.requireValid(name, "Table id name");
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static TableId parse(String value) {
		String[] parts = IdPart.split(value, "Table id", 2);
		if (parts.length != 2) {
			throw new IllegalArgumentException("Table id must have exactly two parts: " + value);
		}
		return new TableId(DatasetId.parse(parts[0]), parts[1]);
	}

	public static TableId valueOf(String value) {
		return parse(value);
	}

	@Override
	@JsonValue
	public String toString() {
		return datasetId + "." + name;
	}
}
