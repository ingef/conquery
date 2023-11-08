package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;

class AggregationSelectCte extends ConceptCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(ConceptCteContext conceptCteContext) {

		List<SqlSelect> requiredInAggregationFilterStep = conceptCteContext.allConceptSelects()
																		   .flatMap(sqlSelects -> sqlSelects.getForAggregationSelectStep().stream())
																		   .distinct()
																		   .collect(Collectors.toList());

		Selects aggregationSelectSelects = new Selects(conceptCteContext.getPrimaryColumn(), requiredInAggregationFilterStep);

		return QueryStep.builder()
						.selects(aggregationSelectSelects)
						.groupBy(List.of(conceptCteContext.getPrimaryColumn()));
	}

	@Override
	public ConceptStep cteStep() {
		return ConceptStep.AGGREGATION_SELECT;
	}

}
