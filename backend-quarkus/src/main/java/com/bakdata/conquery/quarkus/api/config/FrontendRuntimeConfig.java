package com.bakdata.conquery.quarkus.api.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "conquery.frontend")
public interface FrontendRuntimeConfig {

	/**
	 * Currency formatting options returned by the frontend configuration endpoint.
	 */
	Currency currency();

	/**
	 * Query upload id-column configuration returned by the frontend configuration endpoint.
	 */
	QueryUpload queryUpload();

	/**
	 * Number of years before the current year used to derive the observation period start.
	 */
	@WithDefault("6")
	int observationPeriodYears();

	/**
	 * Optional manual/help URL shown by the frontend.
	 */
	Optional<String> manualUrl();

	/**
	 * Optional support contact email shown by the frontend.
	 */
	Optional<String> contactEmail();

	interface Currency {
		/**
		 * Currency unit or symbol.
		 */
		@WithDefault("€")
		String unit();

		/**
		 * Thousands separator for currency values.
		 */
		@WithDefault(".")
		String thousandSeparator();

		/**
		 * Decimal separator for currency values.
		 */
		@WithDefault(",")
		String decimalSeparator();

		/**
		 * Number of fraction digits for currency values.
		 */
		@WithDefault("2")
		int decimalScale();
	}

	interface QueryUpload {
		/**
		 * Logical table name used by the frontend query upload workflow.
		 */
		@WithDefault("entities")
		String table();

		/**
		 * Entity id columns accepted by the frontend query upload workflow.
		 */
		Optional<List<IdColumn>> ids();
	}

	interface IdColumn {
		/**
		 * Display name of the upload id column.
		 */
		@WithDefault("ID")
		String name();

		/**
		 * Field name used in uploaded rows.
		 */
		@WithDefault("pid")
		String field();

		/**
		 * Localized labels for this id column.
		 */
		Map<String, String> label();

		/**
		 * Whether this id column is the primary entity id.
		 */
		@WithDefault("true")
		boolean primaryId();

		/**
		 * Whether this id column should be printed in uploaded query result displays.
		 */
		@WithDefault("true")
		boolean print();
	}
}
