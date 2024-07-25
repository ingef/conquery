package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.SqlTables;

abstract class ConnectorCte {


	protected Optional<QueryStep> convert(CQTableContext tableContext, Optional<QueryStep> previous) {

		if (!isRequired(tableContext.getConnectorTables())) {
			return Optional.empty();
		}

		String cteName = tableContext.getConnectorTables().cteName(cteStep());
		QueryStep.QueryStepBuilder queryStepBuilder = this.convertStep(tableContext).cteName(cteName);

		// only preprocessing has no predecessor
		if (previous.isEmpty()) {
			queryStepBuilder.predecessors(List.of());
		}
		// if interval packing takes place, fromTable and predecessors of the final concept step are already set
		else if (queryStepBuilder.build().getFromTables().isEmpty() && queryStepBuilder.build().getPredecessors().isEmpty()) {
			queryStepBuilder.fromTable(QueryStep.toTableLike(previous.get().getCteName()))
							.predecessors(List.of(previous.get()));
		}
		return Optional.of(queryStepBuilder.build());
	}

	/**
	 * @return The {@link ConceptCteStep} this instance belongs to.
	 */
	protected abstract ConceptCteStep cteStep();

	protected abstract QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext);

	private boolean isRequired(SqlTables connectorTables) {
		return connectorTables.isRequiredStep(cteStep());
	}

}
