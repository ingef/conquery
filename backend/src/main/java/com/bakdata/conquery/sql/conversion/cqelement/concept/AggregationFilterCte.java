package com.bakdata.conquery.sql.conversion.cqelement.concept;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorCteCompiler;

class AggregationFilterCte extends ConnectorCte {

	@Override
	public ConceptCteStep cteStep() {
		return ConceptCteStep.AGGREGATION_FILTER;
	}

	@Override
	public QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {
		return ConnectorCteCompiler.compileAggregationFilter(
				tableContext.getPrevious(),
				tableContext.allSqlSelects(),
				tableContext.getSqlFilters()
		);
	}

}
