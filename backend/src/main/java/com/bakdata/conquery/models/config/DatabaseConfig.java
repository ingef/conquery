package com.bakdata.conquery.models.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

/**
 * Connection properties for a SQL database.
 * <p/>
 * Currently supported are HANA and Prostgres databases, see {@link DatabaseConfig#dialect}.
 */
@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseConfig {

	private static final String DEFAULT_PRIMARY_COLUMN = "pid";

	/**
	 * SQL vendor specific dialect used to transform queries to SQL
	 */
	private Dialect dialect;

	/**
	 * Username used to connect to the database.
	 */
	private String databaseUsername;


	/**
	 * Password used to connect to the database.
	 */
	private String databasePassword;

	/**
	 * Connections url in JDBC notation.
	 */
	private String jdbcConnectionUrl;

	/**
	 * Name of the column which is shared among the table and all aggregations are grouped by.
	 */
	@Builder.Default
	private String primaryColumn = DEFAULT_PRIMARY_COLUMN;

}
