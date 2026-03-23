package com.bakdata.conquery.quarkus.api.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "conquery.frontend")
public interface FrontendRuntimeConfig {

	Currency currency();
	QueryUpload queryUpload();

	@WithDefault("6")
	int observationPeriodYears();

	Optional<String> manualUrl();

	Optional<String> contactEmail();

	interface Currency {
		@WithDefault("€")
		String unit();

		@WithDefault(".")
		String thousandSeparator();

		@WithDefault(",")
		String decimalSeparator();

		@WithDefault("2")
		int decimalScale();
	}

	interface QueryUpload {
		@WithDefault("entities")
		String table();

		Optional<List<IdColumn>> ids();
	}

	interface IdColumn {
		@WithDefault("ID")
		String name();

		@WithDefault("pid")
		String field();

		Map<String, String> label();

		@WithDefault("true")
		boolean primaryId();

		@WithDefault("true")
		boolean print();
	}
}
