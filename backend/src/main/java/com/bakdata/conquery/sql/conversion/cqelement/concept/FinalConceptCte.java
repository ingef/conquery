package com.bakdata.conquery.sql.conversion.cqelement.concept;

import com.bakdata.conquery.sql.conversion.context.step.QueryStep;

class FinalConceptCte extends ConceptCte {

	@Override
	protected boolean canConvert(CteContext cteContext) {
		return true;
	}

	@Override
	protected QueryStep.QueryStepBuilder convertStep(CteContext cteContext) {
		return QueryStep.builder()
						.selects(cteContext.getPrevious().getQualifiedSelects());
	}

	@Override
	protected CteStep cteStep() {
		return CteStep.FINAL;
	}

}
