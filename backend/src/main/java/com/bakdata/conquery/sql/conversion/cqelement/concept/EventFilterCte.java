package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.filter.FilterCondition;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Condition;

class EventFilterCte extends ConceptCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(ConceptCteContext conceptCteContext) {

		String predecessorTableName = conceptCteContext.getConceptTables().getPredecessor(cteStep());
		Selects eventFilterSelects = Selects.builder()
											.primaryColumn(conceptCteContext.getPrimaryColumn())
											.validityDate(conceptCteContext.getValidityDate())
											.sqlSelects(getForAggregationSelectStep(conceptCteContext))
											.build()
											.qualify(predecessorTableName);

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
								.toList();
	}

	@Override
	public ConceptCteStep cteStep() {
		return ConceptCteStep.EVENT_FILTER;
	}

}
