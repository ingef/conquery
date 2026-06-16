package com.bakdata.conquery.quarkus.api.config;

import java.util.List;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "conquery")
public interface TablesRuntimeConfig {
	/**
	 * Static table definitions used when no generated metadata folder is mounted.
	 */
	Optional<List<TableEntry>> tables();

	interface TableEntry {
		/**
		 * Table id as exposed to frontend concept details.
		 */
		String id();

		/**
		 * Human-readable table label shown in frontend concept details.
		 */
		String label();

		/**
		 * Dataset id this table belongs to.
		 */
		String dataset();

		/**
		 * Optional primary column id for entity identity in this table.
		 */
		@WithDefault("")
		String primaryColumn();

		/**
		 * Column metadata available for filters, selects, and secondary id resolution.
		 */
		List<ColumnEntry> columns();
	}

	interface ColumnEntry {
		/**
		 * Column id as exposed in frontend table/filter/select models.
		 */
		String id();

		/**
		 * Human-readable column label.
		 */
		String label();

		/**
		 * Conquery column type, for example STRING, INTEGER, DATE, MONEY, or DATE_RANGE.
		 */
		String type();

		/**
		 * Optional secondary id category represented by this column.
		 */
		@WithDefault("__unset__")
		String secondaryId();
	}
}
