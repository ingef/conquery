package com.bakdata.conquery.quarkus.ids;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record DatasetId(String name) {

	public DatasetId {
		name = IdPart.requireValid(name, "Dataset id");
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static DatasetId parse(String value) {
		return new DatasetId(IdPart.split(value, "Dataset id", 1)[0]);
	}

	public static DatasetId valueOf(String value) {
		return parse(value);
	}

	@Override
	@JsonValue
	public String toString() {
		return name;
	}
}
