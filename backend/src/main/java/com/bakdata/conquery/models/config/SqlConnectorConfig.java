package com.bakdata.conquery.models.config;

import java.util.Map;
import jakarta.validation.Valid;

import com.bakdata.conquery.mode.local.ConnectionManager;
import com.bakdata.conquery.mode.local.ManagedConnection;
import com.bakdata.conquery.models.datasets.Dataset;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.validation.ValidationMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class SqlConnectorConfig {

	private boolean enabled;

	/**
	 * Determines if generated SQL should be formatted.
	 */
	private boolean withPrettyPrinting;

	/**
	 * Keys must match the name of existing {@link Dataset}s.
	 */
	private Map<String, @Valid DatabaseConnectionConfig> databaseConfigs;


	public ConnectionManager toConnectionManager(Environment environment) {
		ConnectionManager connectionManager = new ConnectionManager();

		for (Map.Entry<String, DatabaseConnectionConfig> configEntry : getDatabaseConfigs().entrySet()) {
			ManagedConnection managedConnection = new ManagedConnection(configEntry.getKey(), this, configEntry.getValue(), environment.healthChecks());

			environment.lifecycle().manage(managedConnection);
			connectionManager.addConnection(configEntry.getKey(), managedConnection);
		}

		return connectionManager;
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
