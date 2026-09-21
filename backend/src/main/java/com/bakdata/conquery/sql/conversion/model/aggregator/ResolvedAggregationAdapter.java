package com.bakdata.conquery.sql.conversion.model.aggregator;

import com.bakdata.conquery.sql.compiler.conversion.operation.AggregationConversionContext;
import com.bakdata.conquery.sql.compiler.conversion.operation.ResolvedAggregationConverter;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.concept.CommonAggregationSelect;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.model.operation.ResolvedAggregation;

/** Passes resolved operations and the existing naming/table state to the shared compiler. */
final class ResolvedAggregationAdapter {

	private static final ResolvedAggregationConverter CONVERTER = new ResolvedAggregationConverter();

	private ResolvedAggregationAdapter() {
	}

	static CommonAggregationSelect<?> convert(ResolvedAggregation aggregation, String alias, SqlIdColumns ids, SqlTables tables, Context context) {
		return CONVERTER.convert(aggregation, new AggregationConversionContext(
				context.getCompilerDialect(), context.getNameGenerator(), tables, ids, alias));
	}
}
