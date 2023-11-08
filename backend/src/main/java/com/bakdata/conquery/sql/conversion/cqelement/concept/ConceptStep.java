package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.sql.conversion.model.CteStep;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ConceptStep implements CteStep {

	PREPROCESSING("_preprocessing", null),
	EVENT_FILTER("_event_filter", PREPROCESSING),
	AGGREGATION_SELECT("_group_select", EVENT_FILTER),
	AGGREGATION_FILTER("_group_filter", AGGREGATION_SELECT),
	FINAL("", AGGREGATION_FILTER);

	public static final Set<ConceptStep> MANDATORY_STEPS = Set.of(ConceptStep.PREPROCESSING, ConceptStep.AGGREGATION_SELECT, ConceptStep.FINAL);

	private final String suffix;
	private final ConceptStep predecessor;

	public static Set<ConceptStep> withOptionalSteps(ConceptStep... conceptStep) {
		HashSet<ConceptStep> steps = new HashSet<>(MANDATORY_STEPS);
		steps.addAll(Set.of(conceptStep));
		return steps;
	}

	@Override
	public String cteName(String conceptLabel) {
		return "concept_%s%s".formatted(conceptLabel, this.suffix);
	}

	public ConceptStep predecessor() {
		return this.predecessor;
	}

}
