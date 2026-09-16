package com.bakdata.conquery.quarkus.plugin.api.filters;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public final class UnknownFilterDefinition implements FilterDefinition {

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
}
