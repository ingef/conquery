package com.bakdata.conquery.models.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlConnectorConfig {

	public static final String DEFAULT_PRIMARY_COLUMN = "pid";

	boolean enabled;

	private Dialect dialect;

	/**
	 * Determines if generated SQL should be formatted.
	 */
	private boolean withPrettyPrinting;

	private String databaseUsername;

	private String databasePassword;

	private String jdbcConnectionUrl;

	private String primaryColumn = DEFAULT_PRIMARY_COLUMN;
}
