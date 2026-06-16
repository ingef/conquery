package com.bakdata.conquery.quarkus.api.config;

import java.util.List;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "conquery.form-queries")
public interface FormQueriesRuntimeConfig {
	/**
	 * Classpath resources containing frontend form query configuration payloads.
	 */
	@WithDefault("forms/export_form.frontend_conf.json,forms/table_export_form.frontend_conf.json")
	List<String> resources();
}
