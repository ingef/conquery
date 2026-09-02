package com.bakdata.conquery.quarkus.ids;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record FilterId(ConnectorId connectorId, String name) {

	public FilterId {
		if (connectorId == null) {
			throw new IllegalArgumentException("Filter id connector must not be null.");
		}
		name = IdPart.requireValid(name, "Filter id name");
	}

	public DatasetId datasetId() {
		return connectorId.datasetId();
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static FilterId parse(String value) {
		String[] parts = IdPart.split(value, "Filter id", 3);
		String filterName = parts[parts.length - 1];
		String connectorName = parts[parts.length - 2];
		ConceptId conceptId = new ConceptId(DatasetId.parse(parts[0]), Arrays.stream(parts, 1, parts.length - 2).toList());
		return new FilterId(new ConnectorId(conceptId, connectorName), filterName);
	}

	public static FilterId valueOf(String value) {
		return parse(value);
	}

	@Override
	@JsonValue
	public String toString() {
		return connectorId + "." + name;
	}
}
