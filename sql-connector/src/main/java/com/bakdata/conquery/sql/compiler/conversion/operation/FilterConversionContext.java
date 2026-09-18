package com.bakdata.conquery.sql.compiler.conversion.operation;

import java.util.Objects;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;

/** Compiler state available while converting a resolved filter into connector IR. */
public record FilterConversionContext(
		CompilerDialect dialect,
		SqlNameGenerator nameGenerator,
		SqlTables tables,
		SqlIdColumns ids
) {

	public FilterConversionContext {
		Objects.requireNonNull(dialect, "dialect");
		Objects.requireNonNull(nameGenerator, "nameGenerator");
		Objects.requireNonNull(tables, "tables");
		Objects.requireNonNull(ids, "ids");
	}
}
