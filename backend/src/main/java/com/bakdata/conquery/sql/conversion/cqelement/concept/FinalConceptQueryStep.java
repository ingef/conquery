package com.bakdata.conquery.sql.conversion.cqelement.concept;

import com.bakdata.conquery.sql.conversion.context.selects.Selects;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;

class FinalConceptQueryStep extends ConceptQueryStep {

	@Override
	public boolean canConvert(StepContext stepContext) {
		return true;
	}

	@Override
	public QueryStep.QueryStepBuilder convertStep(StepContext stepContext) {
		Selects finalSelects = stepContext.getPrevious().getQualifiedSelects();
		return QueryStep.builder().selects(finalSelects);
	}

	@Override
	public String nameSuffix() {
		return "";
	}
}
