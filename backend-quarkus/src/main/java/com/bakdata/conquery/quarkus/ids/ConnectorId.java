package com.bakdata.conquery.quarkus.ids;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record ConnectorId(ConceptId conceptId, String name) {

	public ConnectorId {
		if (conceptId == null) {
			throw new IllegalArgumentException("Connector id concept must not be null.");
		}
		name = IdPart.requireValid(name, "Connector id name");
	}

	public DatasetId datasetId() {
		return conceptId.datasetId();
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static ConnectorId parse(String value) {
		String[] parts = IdPart.split(value, "Connector id", 2);
		String connectorName = parts[parts.length - 1];
		ConceptId conceptId = new ConceptId(DatasetId.parse(parts[0]), Arrays.stream(parts, 1, parts.length - 1).toList());
		return new ConnectorId(conceptId, connectorName);
	}

	public static ConnectorId valueOf(String value) {
		return parse(value);
	}

	@Override
	@JsonValue
	public String toString() {
		return conceptId + "." + name;
	}
}
