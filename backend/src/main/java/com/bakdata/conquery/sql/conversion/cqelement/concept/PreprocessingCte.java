package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Optional;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorCteCompiler;
import com.bakdata.conquery.sql.compiler.ir.concept.PreprocessingCteInput;

class PreprocessingCte extends ConnectorCte {

	@Override
	public ConceptCteStep cteStep() {
		return ConceptCteStep.PREPROCESSING;
	}

	@Override
	public QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {
		PreprocessingCteInput input = new PreprocessingCteInput(
				tableContext.getConnectorTables().getPredecessor(ConceptCteStep.PREPROCESSING),
				tableContext.getIds(),
				tableContext.getRawValidityDate(),
				tableContext.getValidityDate(),
				tableContext.allSqlSelects(),
				tableContext.getSqlFilters(),
				Optional.ofNullable(tableContext.getConversionContext().getStratificationTable())
		);
		return ConnectorCteCompiler.compilePreprocessing(input);
	}

}
