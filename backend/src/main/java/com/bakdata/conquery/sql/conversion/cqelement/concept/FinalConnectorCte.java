package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;

class FinalConnectorCte extends ConnectorCte {

	@Override
	protected QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {

		List<SqlSelect> forFinalStep = tableContext.allSqlSelects().stream()
												   .flatMap(sqlSelects -> sqlSelects.getFinalSelects().stream())
												   .toList();

		QueryStep previous = tableContext.getPrevious();
		Selects previousSelects = previous.getQualifiedSelects();
		Selects finalConceptSelects = Selects.builder()
											 .primaryColumn(previousSelects.getPrimaryColumn())
											 .validityDate(previousSelects.getValidityDate())
											 .sqlSelects(forFinalStep)
											 .build();

		return QueryStep.builder().selects(finalConceptSelects);
	}

	@Override
	protected ConnectorCteStep cteStep() {
		return ConnectorCteStep.FINAL;
	}

}
