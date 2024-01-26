package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Condition;

class PreprocessingCte extends ConnectorCte {

	public QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {

		List<SqlSelect> forPreprocessing = tableContext.allConceptSelects()
													   .flatMap(sqlSelects -> sqlSelects.getPreprocessingSelects().stream())
													   .distinct()
													   .toList();

		Selects preprocessingSelects = Selects.builder()
											  .primaryColumn(tableContext.getPrimaryColumn())
											  .validityDate(tableContext.getValidityDate())
											  .sqlSelects(forPreprocessing)
											  .build();

		// all where clauses that don't require any preprocessing (connector/child conditions)
		List<Condition> conditions = tableContext.getFilters().stream()
												 .flatMap(sqlFilter -> sqlFilter.getWhereClauses().getPreprocessingConditions().stream())
												 .map(WhereCondition::condition)
												 .toList();

		return QueryStep.builder()
						.selects(preprocessingSelects)
						.conditions(conditions)
						.fromTable(QueryStep.toTableLike(tableContext.getConceptTables().getPredecessor(ConnectorCteStep.PREPROCESSING)));
	}

	@Override
	public ConnectorCteStep cteStep() {
		return ConnectorCteStep.PREPROCESSING;
	}

}
