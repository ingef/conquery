package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.filter.FilterCondition;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Condition;

class EventFilterCte extends ConceptCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(ConceptCteContext conceptCteContext) {

		Selects eventFilterSelects = Selects.qualified(
				conceptCteContext.getConceptTables().getPredecessorTableName(ConceptCteStep.EVENT_FILTER),
				conceptCteContext.getPrimaryColumn(),
				conceptCteContext.getValidityDate(),
				getForAggregationSelectStep(conceptCteContext)
		);

		List<Condition> eventFilterConditions = conceptCteContext.getFilters().stream()
																 .flatMap(conceptFilter -> conceptFilter.getFilters().getEvent().stream())
																 .map(FilterCondition::filterCondition)
																 .toList();

		return QueryStep.builder()
						.selects(eventFilterSelects)
						.conditions(eventFilterConditions);
	}

	private static List<SqlSelect> getForAggregationSelectStep(ConceptCteContext conceptCteContext) {
		return conceptCteContext.allConceptSelects()
								.flatMap(sqlSelects -> sqlSelects.getForAggregationSelectStep().stream())
								.distinct()
								.collect(Collectors.toList());
	}

	@Override
	public ConceptCteStep cteStep() {
		return ConceptCteStep.EVENT_FILTER;
	}

}
