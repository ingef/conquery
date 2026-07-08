package com.bakdata.conquery.quarkus.concepts.filters.specific;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.quarkus.concepts.filters.StaticFrontendValue;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CommonFilterPayload(
		String type,
		String name,
		String label,
		@JsonAlias("description")
		String tooltip,
		String unit,
		String column,
		String subtractColumn,
		List<String> distinctByColumn,
		List<String> distinctBy,
		Map<String, String> labels,
		List<StaticFrontendValue> options,
		Integer min,
		Integer max,
		String pattern,
		boolean allowDropFile,
		Object defaultValue,
		String timeUnit,
		Map<String, String> flags
) {
}
