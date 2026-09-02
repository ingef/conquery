package com.bakdata.conquery.quarkus.ids;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record StructureNodeId(DatasetId datasetId, List<String> path) {

	public StructureNodeId {
		if (datasetId == null) {
			throw new IllegalArgumentException("Structure node id dataset must not be null.");
		}
		if (path == null || path.isEmpty()) {
			throw new IllegalArgumentException("Structure node id path must not be empty.");
		}
		path = path.stream()
				.map(part -> IdPart.requireValid(part, "Structure node id path part"))
				.toList();
	}

	public StructureNodeId child(String name) {
		List<String> childPath = new java.util.ArrayList<>(path);
		childPath.add(name);
		return new StructureNodeId(datasetId, childPath);
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static StructureNodeId parse(String value) {
		String[] parts = IdPart.split(value, "Structure node id", 2);
		return new StructureNodeId(DatasetId.parse(parts[0]), Arrays.stream(parts).skip(1).toList());
	}

	public static StructureNodeId valueOf(String value) {
		return parse(value);
	}

	@Override
	@JsonValue
	public String toString() {
		return datasetId + "." + String.join(".", path);
	}
}
