package com.bakdata.conquery.models.config;

import javax.validation.constraints.Min;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlConnectorConfig {

	boolean enabled;

	private Dialect dialect;

	/**
	 * Determines if generated SQL should be formatted.
	 */
	private boolean withPrettyPrinting;

	/**
	 * Set's the max length of database identifiers (column names, qualifiers, etc.).
	 */
	@Min(63)
	private int nameMaxLength;

	private String databaseUsername;

	private String databasePassword;

	private String jdbcConnectionUrl;

	private String primaryColumn = "pid";
}
