package com.bakdata.conquery.models.config;

import com.bakdata.conquery.sql.conversion.dialect.DialectBundle;
import com.bakdata.conquery.sql.conversion.dialect.HanaDialectBundle;
import com.bakdata.conquery.sql.conversion.dialect.PostgreDialectBundle;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
	POSTGRESQL(new PostgreDialectBundle()),
	/**
	 * Dialect for SAP HANA database
	 */
	HANA(new HanaDialectBundle());

	private final DialectBundle dialectBundle;
}
