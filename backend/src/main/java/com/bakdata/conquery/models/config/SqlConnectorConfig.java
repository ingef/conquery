package com.bakdata.conquery.models.config;

import java.util.Map;

import com.bakdata.conquery.models.datasets.Dataset;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.ValidationMethod;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

/**
 * Configuration for SQL databases to send dataset queries to.
 * <p/>
 * Multiple databases can be configured for different datasets.
 *
 * @implNote At the moment, dataset names are statically mapped to a database by the {@link SqlConnectorConfig#databaseConfigs}-map.
 */
@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class SqlConnectorConfig {

	private boolean enabled;

	/**
	 * Determines if generated SQL should be formatted.
	 */
	private boolean withPrettyPrinting;

	/**
	 * Keys must match the name of existing {@link Dataset}s.
	 */
	private Map<String, @Valid DatabaseConfig> databaseConfigs;

	/**
	 * Timeout duration after which a database connection is considered unhealthy (defaults to connection timeout)
	 */
	private Duration connectivityCheckTimeout;

	public DatabaseConfig getDatabaseConfig(Dataset dataset) {
		return databaseConfigs.get(dataset.getName());
	}

	@JsonIgnore
	@ValidationMethod(message = "At lease 1 DatabaseConfig has to be present if SqlConnector config is enabled")
	public boolean isValidSqlConnectorConfig() {
		if (!enabled) {
			return true;
		}
		return databaseConfigs != null && !databaseConfigs.isEmpty();
	}

}
