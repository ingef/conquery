package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.context.step.QueryStep;

abstract class ConceptCte {

	protected Optional<QueryStep> convert(CteContext context, Optional<QueryStep> previous) {

		if (!canConvert(context)) {
			return Optional.empty();
		}

		String cteName = context.getConceptTableNames().tableNameFor(cteStep());
		QueryStep.QueryStepBuilder queryStepBuilder = this.convertStep(context).cteName(cteName);

		if (previous.isPresent()) {
			queryStepBuilder.predecessors(List.of(previous.get()))
							.fromTable(QueryStep.toTableLike(previous.get().getCteName()));
		}
		else {
			queryStepBuilder.predecessors(Collections.emptyList())
							.fromTable(QueryStep.toTableLike(context.getConceptTableNames().rootTable()));
		}
		return Optional.of(queryStepBuilder.build());

	}

	protected abstract boolean canConvert(CteContext cteContext);

	/**
	 * @return The {@link CteStep} this instance belongs to.
	 */
	protected abstract CteStep cteStep();

	protected abstract QueryStep.QueryStepBuilder convertStep(CteContext cteContext);

}
