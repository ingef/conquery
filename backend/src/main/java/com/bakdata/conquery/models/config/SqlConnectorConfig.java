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

	/**
	 * The amount of threads for background tasks like calculating matching stats {@link com.bakdata.conquery.models.jobs.SqlUpdateMatchingStatsJob}.
	 */
	@Min(1)
	private int backgroundThreads;
}
