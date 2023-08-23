package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.context.step.QueryStep;

abstract class ConceptQueryStep {

	public Optional<QueryStep> convert(StepContext context) {
		if (!canConvert(context)) {
			return Optional.empty();
		}

		QueryStep.QueryStepBuilder queryStepBuilder = this.convertStep(context).cteName(createCteName(context));

		if (context.getPrevious() != null) {
			queryStepBuilder.predecessors(List.of(context.getPrevious()))
							.fromTable(QueryStep.toTableLike(context.getPrevious().getCteName()));
		}
		else {
			queryStepBuilder.predecessors(Collections.emptyList())
							.fromTable(QueryStep.toTableLike(context.getTable().getConnector().getTable().getName()));
		}
		return Optional.of(queryStepBuilder.build());

	}

	abstract boolean canConvert(StepContext stepContext);

	abstract QueryStep.QueryStepBuilder convertStep(StepContext stepContext);

	abstract String nameSuffix();

	private String createCteName(StepContext stepContext) {
		return "concept_%s%s".formatted(stepContext.getConceptLabel(), nameSuffix());
	}

}
