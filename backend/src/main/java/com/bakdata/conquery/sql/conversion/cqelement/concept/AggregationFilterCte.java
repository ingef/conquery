package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.sql.conversion.model.QualifyingUtil;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Condition;
import org.jooq.Field;

class AggregationFilterCte extends ConceptCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(ConceptCteContext conceptCteContext) {

		String predecessorTableName = conceptCteContext.getConceptTables().getPredecessor(cteStep());
		Field<Object> primaryColumn = QualifyingUtil.qualify(conceptCteContext.getPrimaryColumn(), predecessorTableName);
		Selects aggregationFilterSelects = Selects.builder()
												  .primaryColumn(primaryColumn)
												  .sqlSelects(getForAggregationFilterSelects(conceptCteContext))
												  .build()
												  .qualify(predecessorTableName);

		List<Condition> aggregationFilterConditions = conceptCteContext.getFilters().stream()
																	   .flatMap(conceptFilter -> conceptFilter.getWhereClauses().getGroupFilters().stream())
																	   .map(WhereCondition::condition)
																	   .toList();

		return QueryStep.builder()
						.selects(aggregationFilterSelects)
						.conditions(aggregationFilterConditions);
	}

	private List<SqlSelect> getForAggregationFilterSelects(ConceptCteContext conceptCteContext) {
		return conceptCteContext.getSelects().stream()
								.flatMap(sqlSelects -> sqlSelects.getFinalSelects().stream())
								.filter(sqlSelect -> !sqlSelect.isUniversal())
								.distinct()
								.toList();
	}

	@Override
	public ConceptCteStep cteStep() {
		return ConceptCteStep.AGGREGATION_FILTER;
	}

}
