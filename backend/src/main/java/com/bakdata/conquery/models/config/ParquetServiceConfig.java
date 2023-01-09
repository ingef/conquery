package com.bakdata.conquery.models.config;


import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.service.AbstractResultProviderConfig;

@CPSType(base = PluginConfig.class, id = "PARQUET")
public class ParquetServiceConfig extends AbstractResultProviderConfig {
	public ParquetServiceConfig() {
		super(false, 0);
	}

	public ParquetServiceConfig(boolean hidden, int priority) {
		super(hidden, priority);
	}
}
