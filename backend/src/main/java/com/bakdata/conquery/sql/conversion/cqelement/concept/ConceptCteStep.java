package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.sql.conversion.model.CteStep;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ConceptCteStep implements CteStep {

	PREPROCESSING("_preprocessing", null),
	EVENT_FILTER("_event_filter", PREPROCESSING),
	AGGREGATION_SELECT("_group_select", EVENT_FILTER),
	AGGREGATION_FILTER("_group_filter", AGGREGATION_SELECT),
	FINAL("", AGGREGATION_FILTER);

	public static final Set<ConceptCteStep> MANDATORY_STEPS = Set.of(ConceptCteStep.PREPROCESSING, ConceptCteStep.AGGREGATION_SELECT, ConceptCteStep.FINAL);

	private final String suffix;
	private final ConceptCteStep predecessor;

	public static Set<ConceptCteStep> withOptionalSteps(ConceptCteStep... conceptCteStep) {
		HashSet<ConceptCteStep> steps = new HashSet<>(MANDATORY_STEPS);
		steps.addAll(Set.of(conceptCteStep));
		return steps;
	}

	@Override
	public String cteName(String conceptLabel) {
		return "concept_%s%s".formatted(conceptLabel, this.suffix);
	}

	public ConceptCteStep predecessor() {
		return this.predecessor;
	}

}
