package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.context.selects.ConceptSelects;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import org.jooq.Condition;

class DateRestrictionQueryStep implements ConceptQueryStep {

	@Override
	public boolean canConvert(StepContext stepContext) {
		return stepContext.getPreviousSelects().getDateRestrictionRange().isPresent();
	}

	@Override
	public QueryStep.QueryStepBuilder convertStep(StepContext stepContext) {
		ConceptSelects dateRestrictionSelects = this.prepareDateRestrictionSelects(stepContext);
		Condition dateRestriction = stepContext.getSqlFunctions().dateRestriction(
				stepContext.getPreviousSelects().getDateRestrictionRange().get(),
				stepContext.getPreviousSelects().getValidityDate().get()
		);

		return QueryStep.builder()
						.selects(dateRestrictionSelects)
						.conditions(List.of(dateRestriction));
	}

	@Override
	public String nameSuffix() {
		return "_date_restriction";
	}

	private ConceptSelects prepareDateRestrictionSelects(final StepContext stepContext) {
		ConceptSelects.ConceptSelectsBuilder selectsBuilder = stepContext.getPreviousSelects().toBuilder();
		selectsBuilder.dateRestrictionRange(Optional.empty());
		if (stepContext.getNode().isExcludeFromTimeAggregation()) {
			selectsBuilder.validityDate(Optional.empty());
		}
		return selectsBuilder.build();
	}

}
