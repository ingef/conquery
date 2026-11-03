package com.bakdata.conquery.sql.translate.conversion.aggregation;

import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import com.bakdata.conquery.sql.translate.ir.SqlTables;
import com.bakdata.conquery.sql.translate.naming.SqlNameGenerator;
import lombok.NonNull;

/** Compiler state available while converting a resolved aggregation into connector IR. */
public record AggregationConversionContext(
		@NonNull TranslatorDialect dialect,
		@NonNull SqlNameGenerator nameGenerator,
		@NonNull SqlTables tables,
		@NonNull SqlIdColumns ids,
		@NonNull String alias
) {

	public AggregationConversionContext {
		if (alias.isBlank()) {
			throw new IllegalArgumentException("alias must not be blank");
		}
	}
}
