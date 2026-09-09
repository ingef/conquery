package com.bakdata.conquery.quarkus.ids;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record ConceptSelectId(ConceptId conceptId, String name) {

	public ConceptSelectId {
		if (conceptId == null) {
			throw new IllegalArgumentException("Concept select id concept must not be null.");
		}
		name = IdPart.requireValid(name, "Concept select id name");
	}

	public DatasetId datasetId() {
		return conceptId.datasetId();
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static ConceptSelectId parse(String value) {
		String[] parts = IdPart.split(value, "Concept select id", 2);
		String selectName = parts[parts.length - 1];
		ConceptId conceptId = new ConceptId(DatasetId.parse(parts[0]), Arrays.stream(parts, 1, parts.length - 1).toList());
		return new ConceptSelectId(conceptId, selectName);
	}

	public static ConceptSelectId valueOf(String value) {
		return parse(value);
	}

	@Override
	@JsonValue
	public String toString() {
		return conceptId + "." + name;
	}
}
