package com.bakdata.conquery.quarkus.ids;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record ValidityDateId(ConnectorId connectorId, String name) {

	public ValidityDateId {
		if (connectorId == null) {
			throw new IllegalArgumentException("Validity date id connector must not be null.");
		}
		name = IdPart.requireValid(name, "Validity date id name");
	}

	public DatasetId datasetId() {
		return connectorId.datasetId();
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static ValidityDateId parse(String value) {
		String[] parts = IdPart.split(value, "Validity date id", 3);
		String validityDateName = parts[parts.length - 1];
		String connectorName = parts[parts.length - 2];
		ConceptId conceptId = new ConceptId(DatasetId.parse(parts[0]), Arrays.stream(parts, 1, parts.length - 2).toList());
		return new ValidityDateId(new ConnectorId(conceptId, connectorName), validityDateName);
	}

	public static ValidityDateId valueOf(String value) {
		return parse(value);
	}

	@Override
	@JsonValue
	public String toString() {
		return connectorId + "." + name;
	}
}
