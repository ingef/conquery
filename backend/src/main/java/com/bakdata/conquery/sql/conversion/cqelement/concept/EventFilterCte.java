package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.context.selects.ConceptSelects;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConquerySelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterCondition;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.ExtractingSelect;
import org.jooq.Condition;

class EventFilterCte extends ConceptCte {

	@Override
	public boolean canConvert(CteContext cteContext) {
		return cteContext.getFilters().stream()
						 .anyMatch(filter -> !filter.getFilters().getEvent().isEmpty());
	}

	@Override
	public QueryStep.QueryStepBuilder convertStep(CteContext cteContext) {

		String preprocessingCteName = cteContext.getConceptTableNames().tableNameFor(CteStep.PREPROCESSING);

		ConceptSelects eventFilterSelects = new ConceptSelects(
				cteContext.getPrimaryColumn(),
				cteContext.getValidityDateRange().map(validityDate -> validityDate.qualify(preprocessingCteName)),
				// all selects we need to carry on for later steps have to be selected to be able to reference them from later steps
				getSelectsForAggregationSelectStep(cteContext, preprocessingCteName)
		);

		List<Condition> eventFilterConditions = cteContext.getFilters().stream()
														  .flatMap(conceptFilter -> conceptFilter.getFilters().getEvent().stream())
														  .map(FilterCondition::filterCondition)
														  .toList();

		return QueryStep.builder()
						.selects(eventFilterSelects)
						.conditions(eventFilterConditions);
	}

	private static List<ConquerySelect> getSelectsForAggregationSelectStep(CteContext cteContext, String preprocessingCteName) {
		return cteContext.allConceptSelects()
						 .flatMap(sqlSelects -> sqlSelects.getForAggregationSelectStep().stream())
						 .map(conquerySelect -> new ExtractingSelect<>(preprocessingCteName, conquerySelect.alias().getName(), Object.class))
						 .distinct()
						 .collect(Collectors.toList());
	}

	@Override
	public CteStep cteStep() {
		return CteStep.EVENT_FILTER;
	}

}
