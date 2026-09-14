package com.bakdata.conquery.sql.conversion.cqelement.concept;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorCteCompiler;

class AggregationSelectCte extends ConnectorCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {
		return ConnectorCteCompiler.compileAggregationSelect(tableContext.getPrevious(), tableContext.allSqlSelects());
	}

	@Override
	public ConceptCteStep cteStep() {
		return ConceptCteStep.AGGREGATION_SELECT;
	}

}
