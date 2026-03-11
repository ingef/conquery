package com.bakdata.conquery.models.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jooq.SQLDialect;

/**
 * The dialect sets SQL vendor specific query transformation rules.
 * <p/>
 * There is no fallback dialect, so the dialect must fit the targeted database.
 */
@RequiredArgsConstructor
@Getter
public enum Dialect {

	/**
	 * Dialect for PostgreSQL database
	 */
	POSTGRESQL(SQLDialect.POSTGRES, 63, "SELECT 1"),
	/**
	 * Dialect for SAP HANA database
	 */
	HANA(SQLDialect.DEFAULT, 127, "SELECT 1 FROM DUMMY");

	private final SQLDialect jooqDialect;

	/**
	 * Set's the max length of database identifiers (column names, qualifiers, etc.).
	 */
	private final int nameMaxLength;
	private final String testConnection;
}
