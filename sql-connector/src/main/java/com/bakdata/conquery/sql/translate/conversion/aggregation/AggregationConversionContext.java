package com.bakdata.conquery.sql.translate.conversion.aggregation;

import java.util.Objects;

import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import com.bakdata.conquery.sql.translate.ir.SqlTables;
import com.bakdata.conquery.sql.translate.naming.SqlNameGenerator;

/** Compiler state available while converting a resolved aggregation into connector IR. */
public record AggregationConversionContext(
		TranslatorDialect dialect,
		SqlNameGenerator nameGenerator,
		SqlTables tables,
		SqlIdColumns ids,
		String alias
) {

	public AggregationConversionContext {
		Objects.requireNonNull(dialect, "dialect");
		Objects.requireNonNull(nameGenerator, "nameGenerator");
		Objects.requireNonNull(tables, "tables");
		Objects.requireNonNull(ids, "ids");
		Objects.requireNonNull(alias, "alias");
		if (alias.isBlank()) {
			throw new IllegalArgumentException("alias must not be blank");
		}
	}
}
