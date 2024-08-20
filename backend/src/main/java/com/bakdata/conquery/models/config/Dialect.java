package com.bakdata.conquery.models.config;

import lombok.Getter;
import org.jooq.SQLDialect;

/**
 * The dialect sets SQL vendor specific query transformation rules.
 * <p/>
 * There is no fallback dialect, so the dialect must fit the targeted database.
 */
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
