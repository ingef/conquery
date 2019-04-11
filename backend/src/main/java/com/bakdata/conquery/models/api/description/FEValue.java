package com.bakdata.conquery.models.api.description;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * This class represents a values of a SELECT filter.
 */
@Data @AllArgsConstructor
public class FEValue {
	
	private final String label;
	private final String value;
	private Map<String, String> templateValues;
	private String optionValue;

	public FEValue(String label, String value) {
		this.label = label;
		this.value = value;
	}

	public static List<FEValue> fromLabels(Map<String, String> labels) {
		return labels
			.entrySet()
			.stream()
			.map(e->new FEValue(e.getValue(), e.getKey()))
			.collect(Collectors.toList());
	}
}
