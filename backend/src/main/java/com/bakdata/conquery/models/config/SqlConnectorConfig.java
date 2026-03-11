package com.bakdata.conquery.models.config;

import java.util.Map;
import jakarta.validation.Valid;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.Managed;
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
public class SqlConnectorConfig implements Managed {

	private boolean enabled;

	/**
	 * Determines if generated SQL should be formatted.
	 */
	private boolean withPrettyPrinting;

	/**
	 * Keys must match the name of existing {@link Dataset}s.
	 */
	private Map<DatasetId, @Valid DatabaseConnection> databaseConfigs;


	public DatabaseConnection getDatabaseConfig(Dataset dataset) {
		return databaseConfigs.get(dataset.getId());
	}


	public void initialize(Environment environment) {
		if(databaseConfigs == null || !enabled){
			return;
		}

		for (DatabaseConnection connection : databaseConfigs.values()) {
			connection.setHealthCheckRegistry(environment.healthChecks());
			environment.lifecycle().manage(connection);
		}
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
