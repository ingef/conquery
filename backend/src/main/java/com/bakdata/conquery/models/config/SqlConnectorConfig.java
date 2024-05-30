package com.bakdata.conquery.models.config;

import java.util.Map;

import com.bakdata.conquery.models.datasets.Dataset;
import io.dropwizard.validation.ValidationMethod;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class SqlConnectorConfig {

	boolean enabled;

	/**
	 * Determines if generated SQL should be formatted.
	 */
	private boolean withPrettyPrinting;

	/**
	 * Keys must match the name of existing {@link Dataset}s.
	 */
	@Getter(AccessLevel.PRIVATE)
	private Map<String, @Valid DatabaseConfig> databaseConfigs;

	public DatabaseConfig getDatabaseConfig(Dataset dataset) {
		return databaseConfigs.get(dataset.getName());
	}

	@ValidationMethod(message = "At lease 1 DatabaseConfig has to be present if SqlConnector config is enabled")
	public boolean isValidSqlConnectorConfig() {
		if (!enabled) {
			return true;
		}
		return databaseConfigs != null && !databaseConfigs.isEmpty();
	}

}
