package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import java.util.Optional;

import com.bakdata.conquery.sql.conversion.dialect.IntervalPacker;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import lombok.RequiredArgsConstructor;

/**
 * PostgreSql supports interval packing with a native range function.
 * <p>
 * See <a href="https://www.postgresql.org/docs/current/rangetypes.html">&& range function operator</a>
 */
@RequiredArgsConstructor
public class PostgreSqlIntervalPacker implements IntervalPacker {

	private final SqlFunctionProvider functionProvider;

	@Override
	public QueryStep createIntervalPackingSteps(IntervalPackingContext context) {

		String sourceTableName = context.getIntervalPackingTables().getRootTable();
		SqlIdColumns ids = context.getIds().qualify(sourceTableName);
		ColumnDateRange qualifiedValidityDate = context.getValidityDate().qualify(sourceTableName);
		ColumnDateRange aggregatedValidityDate = this.functionProvider.aggregated(qualifiedValidityDate)
																	  .asValidityDateRange(context.getNodeLabel());

		Selects selectsWithAggregatedValidityDate = Selects.builder()
														   .ids(ids)
														   .validityDate(Optional.of(aggregatedValidityDate))
														   .sqlSelects(context.getCarryThroughSelects())
														   .build();

		return QueryStep.builder()
						.cteName(context.getIntervalPackingTables().cteName(IntervalPackingCteStep.INTERVAL_COMPLETE))
						.selects(selectsWithAggregatedValidityDate)
						.fromTable(QueryStep.toTableLike(sourceTableName))
						.groupBy(ids.toFields())
						.predecessors(Optional.ofNullable(context.getPredecessor()).stream().toList())
						.build();
	}

}
