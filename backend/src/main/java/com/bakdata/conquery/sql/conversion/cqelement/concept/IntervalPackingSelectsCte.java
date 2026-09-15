package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.sql.conversion.dialect.LegacyCompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.interval.IntervalPackingSelectCompiler;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;

public class IntervalPackingSelectsCte {

	public static QueryStep forSelect(
			QueryStep withAggregatedDaterange,
			ColumnDateRange daterange,
			SqlSelect select,
			SqlTables tables,
			LegacyCompilerDialect compilerDialect
	) {
		List<QueryStep> predecessors = List.of(withAggregatedDaterange);
		QueryStep directPredecessor = withAggregatedDaterange;

		// we need an additional predecessor to unnest the validity date if it is a single column range
		if (compilerDialect.supportsSingleColumnRanges()) {
			String unnestCteName = tables.cteName(ConceptCteStep.UNNEST_DATE);
			directPredecessor = compilerDialect.getFunctionProvider().unnestDaterange(daterange, withAggregatedDaterange, unnestCteName);
			predecessors = List.of(withAggregatedDaterange, directPredecessor);
		}

		Selects predecessorSelects = directPredecessor.getQualifiedSelects();
		Selects selects = Selects.builder()
								 .ids(predecessorSelects.getIds())
								 .sqlSelect(select)
								 .build();

		return QueryStep.builder()
						.cteName(tables.cteName(ConceptCteStep.INTERVAL_PACKING_SELECTS))
						.selects(selects)
						.fromTable(QueryStep.toTableLike(directPredecessor.getCteName()))
						.groupBy(predecessorSelects.getIds().toFields())
						.predecessors(predecessors)
						.build();
	}

	public static QueryStep forConnector(QueryStep predecessor, CQTableContext cqTableContext) {
		return IntervalPackingSelectCompiler.compile(
				predecessor,
				cqTableContext.getSqlSelects().stream().flatMap(selects -> selects.getEventDateSelects().stream()).toList(),
				cqTableContext.getConnectorTables()
		);
	}

	public static QueryStep forConcept(
			QueryStep predecessor,
			SqlTables tables,
			List<ConceptSqlSelects> sqlSelects
	) {
		return IntervalPackingSelectCompiler.compile(
				predecessor,
				sqlSelects.stream().flatMap(selects -> selects.getEventDateSelects().stream()).toList(),
				tables
		);
	}

}
