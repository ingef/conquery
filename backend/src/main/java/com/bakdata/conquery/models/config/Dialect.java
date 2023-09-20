package com.bakdata.conquery.models.config;

import lombok.Getter;
import org.jooq.SQLDialect;

@Getter
public enum Dialect {

	POSTGRESQL(SQLDialect.POSTGRES),
	HANA(SQLDialect.DEFAULT);

	private final SQLDialect jooqDialect;

	Dialect(SQLDialect jooqDialect) {
		this.jooqDialect = jooqDialect;
	}

}
