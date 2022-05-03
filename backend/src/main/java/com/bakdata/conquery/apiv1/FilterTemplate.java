package com.bakdata.conquery.apiv1;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@AllArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties({"columns"})
@ToString
public class FilterTemplate implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Path to CSV File.
	 */
	private String filePath;

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
