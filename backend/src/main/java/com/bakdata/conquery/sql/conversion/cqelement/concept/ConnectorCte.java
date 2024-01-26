package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.model.QueryStep;

abstract class ConnectorCte {


	protected Optional<QueryStep> convert(CQTableContext context, Optional<QueryStep> previous) {

		if (!isRequired(context.getConceptTables())) {
			return Optional.empty();
		}

		String cteName = context.getConceptTables().cteName(cteStep());
		QueryStep.QueryStepBuilder queryStepBuilder = this.convertStep(context).cteName(cteName);

		// only preprocessing has no previously converted step
		if (previous.isEmpty()) {
			queryStepBuilder.predecessors(List.of());
		}
		// if interval packing takes place, fromTable and predecessors of the final concept step are already set
		else if (queryStepBuilder.build().getFromTable() == null && queryStepBuilder.build().getPredecessors().isEmpty()) {
			queryStepBuilder.fromTable(QueryStep.toTableLike(previous.get().getCteName()))
							.predecessors(List.of(previous.get()));
		}
		return Optional.of(queryStepBuilder.build());
	}

	/**
	 * @return The {@link ConnectorCteStep} this instance belongs to.
	 */
	protected abstract ConnectorCteStep cteStep();

	protected abstract QueryStep.QueryStepBuilder convertStep(CQTableContext CQTableContext);

	private boolean isRequired(ConceptTables conceptTables) {
		return conceptTables.isRequiredStep(cteStep());
	}

}
