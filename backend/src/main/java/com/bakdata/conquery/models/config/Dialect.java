package com.bakdata.conquery.models.config;

import lombok.Getter;
import org.jooq.SQLDialect;

@Getter
public enum Dialect {

	POSTGRESQL(SQLDialect.POSTGRES, 63),
	HANA(SQLDialect.DEFAULT, 127);

	private final SQLDialect jooqDialect;

	/**
	 * Set's the max length of database identifiers (column names, qualifiers, etc.).
	 */
	private final int nameMaxLength;

	Dialect(SQLDialect jooqDialect, int nameMaxLength) {
		this.jooqDialect = jooqDialect;
		this.nameMaxLength = nameMaxLength;
	}

}
