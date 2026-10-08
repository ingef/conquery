package com.bakdata.conquery.sql.compiler.conversion.operation;

import java.util.Objects;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;

/** Compiler state available while converting a resolved filter into connector IR. */
public record FilterConversionContext(
		CompilerDialect dialect,
		SqlNameGenerator nameGenerator,
		SqlTables tables,
		SqlIdColumns ids,
		Optional<ColumnDateRange> stratificationDate
) {
	public FilterConversionContext(CompilerDialect dialect, SqlNameGenerator nameGenerator, SqlTables tables,
								   SqlIdColumns ids) {
		this(dialect, nameGenerator, tables, ids, Optional.empty());
	}

	public FilterConversionContext {
		Objects.requireNonNull(dialect, "dialect");
		Objects.requireNonNull(nameGenerator, "nameGenerator");
		Objects.requireNonNull(tables, "tables");
		Objects.requireNonNull(ids, "ids");
		Objects.requireNonNull(stratificationDate, "stratificationDate");
	}
}
