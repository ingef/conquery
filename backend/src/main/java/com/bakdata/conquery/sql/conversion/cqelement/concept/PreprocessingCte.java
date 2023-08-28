package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.sql.conversion.context.selects.ConceptSelects;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConquerySelect;

class PreprocessingCte extends ConceptCte {

	public boolean canConvert(CteContext cteContext) {
		// We always apply preprocessing to select the required columns
		return true;
	}

	public QueryStep.QueryStepBuilder convertStep(CteContext cteContext) {

		List<ConquerySelect> preprocessingSelects = cteContext.allConceptSelects()
															  .flatMap(sqlSelects -> sqlSelects.getForPreprocessingStep().stream())
															  .distinct()
															  .toList();

		return QueryStep.builder()
						// TODO: check if signature can be simplified
						.selects(new ConceptSelects(cteContext.getPrimaryColumn(), cteContext.getValidityDateRange(), preprocessingSelects))
						.conditions(Collections.emptyList())
						.predecessors(Collections.emptyList());
	}

	@Override
	public CteStep cteStep() {
		return CteStep.PREPROCESSING;
	}

}
