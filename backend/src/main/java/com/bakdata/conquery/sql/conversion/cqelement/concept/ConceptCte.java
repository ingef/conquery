package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.model.QueryStep;

abstract class ConceptCte {

	protected Optional<QueryStep> convert(CteContext context, Optional<QueryStep> previous) {

		if (!isRequired(context.getConceptTables())) {
			return Optional.empty();
		}

		String cteName = context.getConceptTables().cteName(cteStep());
		QueryStep.QueryStepBuilder queryStepBuilder = this.convertStep(context).cteName(cteName);

		if (previous.isPresent()) {
			queryStepBuilder.predecessors(List.of(previous.get()))
							.fromTable(QueryStep.toTableLike(previous.get().getCteName()));
		}
		else {
			// only PREPROCESSING step has no predecessor
			queryStepBuilder.predecessors(Collections.emptyList())
							.fromTable(QueryStep.toTableLike(context.getConceptTables().getPredecessorTableName(CteStep.PREPROCESSING)));
		}
		return Optional.of(queryStepBuilder.build());
	}

	/**
	 * @return The {@link CteStep} this instance belongs to.
	 */
	protected abstract CteStep cteStep();

	protected abstract QueryStep.QueryStepBuilder convertStep(CteContext cteContext);

	private boolean isRequired(ConceptTables conceptTables) {
		return conceptTables.isRequiredStep(cteStep());
	}

}
