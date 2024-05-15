package com.bakdata.conquery.models.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DatabaseConfig {

	public static final String DEFAULT_PRIMARY_COLUMN = "pid";

	private Dialect dialect;

	private String databaseUsername;

	private String databasePassword;

	private String jdbcConnectionUrl;

	private String primaryColumn = DEFAULT_PRIMARY_COLUMN;

}
