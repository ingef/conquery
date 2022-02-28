package com.bakdata.conquery.apiv1;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FilterTemplate implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Path to CSV File.
	 */
	private String filePath;

	/**
	 * Value to Filter.
	 */
	private String columnValue;
	/**
	 * Selected value.
	 */
	private String value;
	/**
	 * Option value.
	 */
	private String optionValue;

}
