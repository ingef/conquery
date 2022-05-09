package com.bakdata.conquery.apiv1;

import java.io.Serializable;
import java.net.URL;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode
public class FilterTemplate implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Path to CSV File.
	 */
	private URL filePath;

	/**
	 * Value to be sent for filtering.
	 */
	private String columnValue;
	/**
	 * Value displayed in Select list. Usually concise display.
	 */
	private String value;
	/**
	 * More detailed value. Displayed when value is selected.
	 */
	private String optionValue;

}
