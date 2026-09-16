package com.bakdata.conquery.quarkus.ids;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record ConceptId(DatasetId datasetId, List<String> path) {

	public ConceptId {
		if (datasetId == null) {
			throw new IllegalArgumentException("Concept id dataset must not be null.");
		}
		path = path == null ? List.of() : path.stream()
											  .map(part -> IdPart.requireValid(part, "Concept id path part"))
											  .toList();
	}

	public ConceptId child(String name) {
		List<String> childPath = new java.util.ArrayList<>(path);
		childPath.add(name);
		return new ConceptId(datasetId, childPath);
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static ConceptId parse(String value) {
		String[] parts = IdPart.split(value, "Concept id", 1);
		return new ConceptId(DatasetId.parse(parts[0]), Arrays.stream(parts).skip(1).toList());
	}

	public static ConceptId valueOf(String value) {
		return parse(value);
	}

	@Override
	@JsonValue
	public String toString() {
		if (path.isEmpty()) {
			return datasetId.toString();
		}
		return datasetId + "." + String.join(".", path);
	}
}
