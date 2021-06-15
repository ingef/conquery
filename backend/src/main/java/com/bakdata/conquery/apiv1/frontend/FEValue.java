package com.bakdata.conquery.apiv1.frontend;

import java.util.Map;

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
}
