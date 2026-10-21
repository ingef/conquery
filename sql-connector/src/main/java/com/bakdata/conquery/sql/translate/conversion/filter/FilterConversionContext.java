package com.bakdata.conquery.sql.translate.conversion.filter;

import java.util.Optional;

import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import com.bakdata.conquery.sql.translate.ir.SqlTables;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.translate.naming.SqlNameGenerator;
import lombok.NonNull;

/** Compiler state available while converting a resolved filter into connector IR. */
public record FilterConversionContext(
		@NonNull TranslatorDialect dialect,
		@NonNull SqlNameGenerator nameGenerator,
		@NonNull SqlTables tables,
		@NonNull SqlIdColumns ids,
		@NonNull Optional<ColumnDateRange> stratificationDate
) {
	public FilterConversionContext(TranslatorDialect dialect, SqlNameGenerator nameGenerator, SqlTables tables,
								   SqlIdColumns ids) {
		this(dialect, nameGenerator, tables, ids, Optional.empty());
	}
}
