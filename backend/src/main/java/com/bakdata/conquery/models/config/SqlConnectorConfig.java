package com.bakdata.conquery.models.config;

import java.util.Map;

import com.bakdata.conquery.models.datasets.Dataset;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Builder
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
	private Map<String, DatabaseConfig> databaseConfigs;

	public DatabaseConfig getDatabaseConfig(Dataset dataset) {
		return databaseConfigs.get(dataset.getName());
	}

}
