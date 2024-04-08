package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Condition;

class AggregationFilterCte extends ConnectorCte {

	@Override
	public ConceptCteStep cteStep() {
		return ConceptCteStep.AGGREGATION_FILTER;
	}

	@Override
	public QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {

		Selects aggregationFilterSelects = getAggregationFilterSelects(tableContext);

		List<Condition> aggregationFilterConditions = tableContext.getSqlFilters().stream()
																  .flatMap(conceptFilter -> conceptFilter.getWhereClauses().getGroupFilters().stream())
																  .map(WhereCondition::condition)
																  .toList();

		return QueryStep.builder()
						.selects(aggregationFilterSelects)
						.conditions(aggregationFilterConditions);
	}

	private Selects getAggregationFilterSelects(CQTableContext tableContext) {

		QueryStep previous = tableContext.getPrevious();
		Selects previousSelects = previous.getQualifiedSelects();
		List<SqlSelect> forAggregationFilterStep =
				tableContext.allSqlSelects().stream()
							.flatMap(sqlSelects -> sqlSelects.getFinalSelects().stream())
							.filter(Predicate.not(SqlSelect::isUniversal))
							.map(sqlSelect -> sqlSelect.qualify(previous.getCteName()))
							.collect(Collectors.toList());

		return Selects.builder()
					  .ids(previousSelects.getIds())
					  .validityDate(previousSelects.getValidityDate())
					  .sqlSelects(forAggregationFilterStep)
					  .build();

	}

}
