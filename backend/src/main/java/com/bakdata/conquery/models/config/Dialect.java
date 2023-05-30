package com.bakdata.conquery.models.config;

import org.jooq.SQLDialect;

public enum Dialect {

	POSTGRESQL(SQLDialect.POSTGRES);

	private final SQLDialect dialect;

	Dialect(SQLDialect dialect) {
		this.dialect = dialect;
	}

	public SQLDialect getJooqDialect() {
		return dialect;
	}

}
