package com.bakdata.conquery.models.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseConfig {

	private static final String DEFAULT_PRIMARY_COLUMN = "pid";

	private Dialect dialect;

	private String databaseUsername;

	private String databasePassword;

	private String jdbcConnectionUrl;

	@Builder.Default
	private String primaryColumn = DEFAULT_PRIMARY_COLUMN;

}
