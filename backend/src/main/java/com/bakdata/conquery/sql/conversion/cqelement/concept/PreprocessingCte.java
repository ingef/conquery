package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;

class PreprocessingCte extends ConceptCte {

	public QueryStep.QueryStepBuilder convertStep(ConceptCteContext conceptCteContext) {

		List<SqlSelect> forPreprocessing = conceptCteContext.allConceptSelects()
															.flatMap(sqlSelects -> sqlSelects.getForPreprocessingStep().stream())
															.distinct()
															.toList();

		Selects preprocessingSelects = new Selects(conceptCteContext.getPrimaryColumn(), conceptCteContext.getValidityDate(), forPreprocessing);

		return QueryStep.builder()
						.selects(preprocessingSelects)
						.fromTable(QueryStep.toTableLike(conceptCteContext.getConceptTables().getPredecessorTableName(ConceptStep.PREPROCESSING)));
	}

	@Override
	public ConceptStep cteStep() {
		return ConceptStep.PREPROCESSING;
	}

}
