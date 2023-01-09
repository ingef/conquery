package com.bakdata.conquery.models.config;


import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.service.AbstractResultProviderConfig;

@CPSType(base = PluginConfig.class, id = "CSV")
public class CsvServiceConfig extends AbstractResultProviderConfig {

	public CsvServiceConfig() {
		super(false, -1);
	}

	public CsvServiceConfig(boolean hidden, int priority) {
		super(hidden, priority);
	}
}
