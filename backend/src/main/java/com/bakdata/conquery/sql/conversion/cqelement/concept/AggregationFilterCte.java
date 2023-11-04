package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.filter.FilterCondition;
import com.bakdata.conquery.sql.conversion.model.select.ExistsSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.ExplicitSelect;
import org.jooq.Condition;

class AggregationFilterCte extends ConceptCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(ConceptCteContext conceptCteContext) {

		String predecessorTableName = conceptCteContext.getConceptTables().getPredecessorTableName(cteStep());
		Selects aggregationFilterSelects = Selects.builder()
												  .primaryColumn(conceptCteContext.getPrimaryColumn())
												  .explicitSelects(getForAggregationFilterSelects(conceptCteContext))
												  .build()
												  .qualify(predecessorTableName);

		List<Condition> aggregationFilterConditions = conceptCteContext.getFilters().stream()
																	   .flatMap(conceptFilter -> conceptFilter.getFilters().getGroup().stream())
																	   .map(FilterCondition::filterCondition)
																	   .toList();

		return QueryStep.builder()
						.selects(aggregationFilterSelects)
						.conditions(aggregationFilterConditions);
	}

	private List<ExplicitSelect> getForAggregationFilterSelects(ConceptCteContext conceptCteContext) {
		return conceptCteContext.getSelects().stream()
								.flatMap(sqlSelects -> sqlSelects.getForFinalStep().stream())
								// TODO: EXISTS edge case is only in a concepts final select statement and has no predecessor selects
								.filter(conquerySelect -> !(conquerySelect instanceof ExistsSqlSelect))
								.distinct()
								.collect(Collectors.toList());
	}

	@Override
	public ConceptCteStep cteStep() {
		return ConceptCteStep.AGGREGATION_FILTER;
	}

}
