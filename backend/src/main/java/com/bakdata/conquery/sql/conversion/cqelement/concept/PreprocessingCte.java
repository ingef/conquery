package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.sql.conversion.context.selects.ConceptSelects;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConquerySelect;

class PreprocessingCte extends ConceptCte {

	public QueryStep.QueryStepBuilder convertStep(CteContext cteContext) {

		List<ConquerySelect> preprocessingSelects = cteContext.allConceptSelects()
															  .flatMap(sqlSelects -> sqlSelects.getForPreprocessingStep().stream())
															  .distinct()
															  .toList();

		return QueryStep.builder()
						.selects(new ConceptSelects(cteContext.getPrimaryColumn(), cteContext.getValidityDateRange(), preprocessingSelects))
						.conditions(Collections.emptyList())
						.predecessors(Collections.emptyList());
	}

	@Override
	public CteStep cteStep() {
		return CteStep.PREPROCESSING;
	}

}
