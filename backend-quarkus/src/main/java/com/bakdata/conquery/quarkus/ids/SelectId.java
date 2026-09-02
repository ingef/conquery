package com.bakdata.conquery.quarkus.ids;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record SelectId(ConnectorId connectorId, String name) {

	public SelectId {
		if (connectorId == null) {
			throw new IllegalArgumentException("Select id connector must not be null.");
		}
		name = IdPart.requireValid(name, "Select id name");
	}

	public DatasetId datasetId() {
		return connectorId.datasetId();
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static SelectId parse(String value) {
		String[] parts = IdPart.split(value, "Select id", 3);
		String selectName = parts[parts.length - 1];
		String connectorName = parts[parts.length - 2];
		ConceptId conceptId = new ConceptId(DatasetId.parse(parts[0]), Arrays.stream(parts, 1, parts.length - 2).toList());
		return new SelectId(new ConnectorId(conceptId, connectorName), selectName);
	}

	public static SelectId valueOf(String value) {
		return parse(value);
	}

	@Override
	@JsonValue
	public String toString() {
		return connectorId + "." + name;
	}
}
