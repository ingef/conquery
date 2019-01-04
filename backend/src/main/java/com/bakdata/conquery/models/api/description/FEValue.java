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
	
	private String value;
	private String label;


	public static List<FEValue> fromLabels(Map<String, String> labels) {
		return labels
			.entrySet()
			.stream()
			.map(e->new FEValue(e.getKey(), e.getValue()))
			.collect(Collectors.toList());
	}
}
