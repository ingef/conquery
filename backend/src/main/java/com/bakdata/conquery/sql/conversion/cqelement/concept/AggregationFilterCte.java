package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.filter.FilterCondition;
import com.bakdata.conquery.sql.conversion.model.select.ExistsSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Condition;

class AggregationFilterCte extends ConceptCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(ConceptCteContext conceptCteContext) {

		Selects aggregationFilterSelects = Selects.qualified(
				conceptCteContext.getConceptTables().getPredecessorTableName(ConceptCteStep.AGGREGATION_FILTER),
				conceptCteContext.getPrimaryColumn(),
				getForAggregationFilterSelects(conceptCteContext)
		);

		List<Condition> aggregationFilterConditions = conceptCteContext.getFilters().stream()
																	   .flatMap(conceptFilter -> conceptFilter.getFilters().getGroup().stream())
																	   .map(FilterCondition::filterCondition)
																	   .toList();

		return QueryStep.builder()
						.selects(aggregationFilterSelects)
						.conditions(aggregationFilterConditions);
	}

	private List<SqlSelect> getForAggregationFilterSelects(ConceptCteContext conceptCteContext) {
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
