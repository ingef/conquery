package com.bakdata.conquery.quarkus.concepts.conditions;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.AssertTrue;

public final class UnknownConceptCondition implements ConceptCondition {

	private String type;
	private final Map<String, Object> properties = new LinkedHashMap<>();

	@Override
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@JsonAnySetter
	public void put(String name, Object value) {
		properties.put(name, value);
	}

	@JsonAnyGetter
	public Map<String, Object> properties() {
		return Map.copyOf(properties);
	}

	@AssertTrue(message = "must use a registered concept condition type")
	public boolean isRegisteredType() {
		return false;
	}
}
