package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.sql.conversion.context.selects.ConceptSelects;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.conversion.filter.FilterConverterService;
import org.jooq.Condition;

public class EventFilterQueryStep implements ConceptQueryStep {

	private final FilterConverterService filterConverterService;

	public EventFilterQueryStep(FilterConverterService filterConverterService) {
		this.filterConverterService = filterConverterService;
	}

	@Override
	public boolean canConvert(StepContext stepContext) {
		return !stepContext.getTable().getFilters().isEmpty();
	}

	@Override
	public QueryStep.QueryStepBuilder convertStep(StepContext stepContext) {

		ConceptSelects eventFilterSelects = stepContext.getPreviousSelects().withEventFilter(Collections.emptyList());
		List<Condition> eventFilterConditions = stepContext.getTable().getFilters().stream()
														   .map(filterValue -> this.filterConverterService.convert(filterValue, stepContext.getContext()))
														   .toList();
		return QueryStep.builder().selects(eventFilterSelects).conditions(eventFilterConditions);
	}

	@Override
	public String nameSuffix() {
		return "_event_filter";
	}

}
