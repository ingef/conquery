package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Condition;

class PreprocessingCte extends ConnectorCte {

	public QueryStep.QueryStepBuilder convertStep(ConceptCteContext conceptCteContext) {

		List<SqlSelect> forPreprocessing = conceptCteContext.allConceptSelects()
															.flatMap(sqlSelects -> sqlSelects.getPreprocessingSelects().stream())
															.distinct()
															.toList();

		Selects preprocessingSelects = Selects.builder()
											  .primaryColumn(conceptCteContext.getPrimaryColumn())
											  .validityDate(conceptCteContext.getValidityDate())
											  .sqlSelects(forPreprocessing)
											  .build();

		// all where clauses that don't require any preprocessing (connector/child conditions)
		List<Condition> conditions = conceptCteContext.getFilters().stream()
													  .flatMap(sqlFilter -> sqlFilter.getWhereClauses().getPreprocessingConditions().stream())
													  .map(WhereCondition::condition)
													  .toList();

		return QueryStep.builder()
						.selects(preprocessingSelects)
						.conditions(conditions)
						.fromTable(QueryStep.toTableLike(conceptCteContext.getConceptTables().getPredecessor(ConnectorCteStep.PREPROCESSING)));
	}

	@Override
	public ConnectorCteStep cteStep() {
		return ConnectorCteStep.PREPROCESSING;
	}

}
