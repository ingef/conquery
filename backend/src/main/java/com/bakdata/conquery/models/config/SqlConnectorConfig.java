package com.bakdata.conquery.models.config;

import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import com.bakdata.conquery.mode.local.SqlUpdateMatchingStatsJob;
import com.bakdata.conquery.models.datasets.Dataset;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.ValidationMethod;
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

	private static final int DEFAULT_BACKGROUND_THREADS = 1;

	private boolean enabled;

	/**
	 * Determines if generated SQL should be formatted.
	 */
	private boolean withPrettyPrinting;

	/**
	 * The amount of threads for background tasks like calculating matching stats {@link SqlUpdateMatchingStatsJob}.
	 */
	@Min(1)
	@Builder.Default
	private int backgroundThreads = DEFAULT_BACKGROUND_THREADS;

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
