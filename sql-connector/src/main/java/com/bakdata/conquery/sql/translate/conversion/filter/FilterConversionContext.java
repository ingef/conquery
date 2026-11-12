package com.bakdata.conquery.sql.translate.conversion.filter;

import java.util.Objects;
import java.util.Optional;

import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import com.bakdata.conquery.sql.translate.ir.SqlTables;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.translate.naming.SqlNameGenerator;

/** Compiler state available while converting a resolved filter into connector IR. */
public record FilterConversionContext(
		TranslatorDialect dialect,
		SqlNameGenerator nameGenerator,
		SqlTables tables,
		SqlIdColumns ids,
		Optional<ColumnDateRange> stratificationDate
) {
	public FilterConversionContext(TranslatorDialect dialect, SqlNameGenerator nameGenerator, SqlTables tables,
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
