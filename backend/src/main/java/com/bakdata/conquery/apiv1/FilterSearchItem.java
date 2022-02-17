package com.bakdata.conquery.apiv1;

import java.io.Serializable;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.map.Flat3Map;

@Getter
@Setter
@NoArgsConstructor
public class FilterSearchItem implements Comparable<FilterSearchItem>, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Template string to be populated by templateValues.
	 */
	private String label;
	private String value;
	private String optionValue;

	private Map<String, String> templateValues = null;

	public void addTemplateColumn(String column, String value) {
		if (templateValues == null){
			templateValues = new Flat3Map<>(); //TODO verify this is useful
		}

		templateValues.put(column, value);
	}

	@Override
	public int compareTo(FilterSearchItem o) {
		return getLabel().compareTo(o.getLabel());
	}

}
