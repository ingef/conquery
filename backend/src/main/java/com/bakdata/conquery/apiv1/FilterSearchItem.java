package com.bakdata.conquery.apiv1;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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


	@Override
	public int compareTo(FilterSearchItem o) {
		return getLabel().compareTo(o.getLabel());
	}

}
