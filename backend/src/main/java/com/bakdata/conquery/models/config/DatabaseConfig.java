package com.bakdata.conquery.models.config;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class DatabaseConfig {

	private static final String DEFAULT_PRIMARY_COLUMN = "pid";

	private Dialect dialect;

	private String databaseUsername;

	private String databasePassword;

	private String jdbcConnectionUrl;

	@Builder.Default
	private String primaryColumn = DEFAULT_PRIMARY_COLUMN;

}
