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
	public QueryStep aggregateAsValidityDate(IntervalPackingContext context) {
		return aggregateDate(context, AggregationMode.VALIDITY_DATE);
	}

	@Override
	public QueryStep aggregateAsArbitrarySelect(IntervalPackingContext context) {
		return aggregateDate(context, AggregationMode.ARBITRARY_SELECT);
	}

	private QueryStep aggregateDate(IntervalPackingContext context, AggregationMode aggregationMode) {

		String sourceTableName = context.getTables().getPredecessor(IntervalPackingCteStep.INTERVAL_COMPLETE);
		SqlIdColumns ids = context.getIds().withAlias().qualify(sourceTableName);
		ColumnDateRange qualifiedDaterange = context.getDaterange().qualify(sourceTableName);
		ColumnDateRange aggregatedDaterange = this.functionProvider.aggregated(qualifiedDaterange);

		Selects.SelectsBuilder selectsBuilder = Selects.builder()
													   .ids(ids)
													   .sqlSelects(context.getCarryThroughSelects());

		switch (aggregationMode) {
			case VALIDITY_DATE -> selectsBuilder.validityDate(Optional.of(aggregatedDaterange));
			case ARBITRARY_SELECT -> selectsBuilder.sqlSelect(aggregatedDaterange);
		}

		return QueryStep.builder()
						.cteName(context.getTables().cteName(IntervalPackingCteStep.INTERVAL_COMPLETE))
						.selects(selectsBuilder.build())
						.fromTable(QueryStep.toTableLike(sourceTableName))
						.groupBy(ids.toFields())
						.predecessors(Optional.ofNullable(context.getPredecessor()).stream().toList())
						.build();
	}


}
