package com.bakdata.conquery.models.config;

import com.bakdata.conquery.sql.conversion.dialect.DialectBundle;
import com.bakdata.conquery.sql.conversion.dialect.clickhouse.ClickhouseDialectBundle;
import com.bakdata.conquery.sql.conversion.dialect.hana.HanaDialectBundle;
import com.bakdata.conquery.sql.conversion.dialect.pg.PostgreDialectBundle;
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

	POSTGRESQL(new PostgreDialectBundle()),
	CLICKHOUSE(new ClickhouseDialectBundle()),
	HANA(new HanaDialectBundle());

	private final DialectBundle dialectBundle;
}
