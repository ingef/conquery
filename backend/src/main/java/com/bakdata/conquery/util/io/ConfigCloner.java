package com.bakdata.conquery.util.io;

import java.io.IOException;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;

public class ConfigCloner {
	public static ConqueryConfig clone(ConqueryConfig config) {
		try {
			ConqueryConfig clone = Jackson.BINARY_MAPPER.readValue(
				Jackson.BINARY_MAPPER.writeValueAsBytes(config),
				ConqueryConfig.class
			);
			clone.setLoggingFactory(config.getLoggingFactory());
			return clone;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to clone a conquery config "+config, e);
		}
	}
}
