package com.bakdata.conquery.sql.compiler.conversion.operation;

import java.util.Objects;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;

/** Compiler state available while converting a resolved aggregation into connector IR. */
public record AggregationConversionContext(
		CompilerDialect dialect,
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
