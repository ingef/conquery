package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.conversion.select.SelectConverterService;
import org.jooq.Field;

class EventSelectStep implements ConceptQueryStep {

	private final SelectConverterService selectConverterService;

	EventSelectStep(SelectConverterService selectConverterService) {
		this.selectConverterService = selectConverterService;
	}

	@Override
	public boolean canConvert(StepContext stepContext) {
		return !stepContext.getTable().getSelects().isEmpty() || !stepContext.getNode().getSelects().isEmpty();
	}

	@Override
	public QueryStep.QueryStepBuilder convertStep(StepContext stepContext) {
		return QueryStep.builder().selects(stepContext.getPreviousSelects().withEventSelect(this.getEventSelects(stepContext)));
	}

	@Override
	public String nameSuffix() {
		return "_event_select";
	}

	@SuppressWarnings("unchecked")
	private List<Field<Object>> getEventSelects(StepContext stepContext) {
		return Stream.concat(stepContext.getTable().getSelects().stream(), stepContext.getNode().getSelects().stream())
					 .map(select -> (Field<Object>) this.selectConverterService.convert(select, stepContext.getContext()))
					 .toList();
	}
}
