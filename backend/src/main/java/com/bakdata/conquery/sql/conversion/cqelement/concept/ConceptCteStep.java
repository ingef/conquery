package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.sql.conversion.model.CteStep;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ConceptCteStep implements CteStep {

	PREPROCESSING("_preprocessing"),
	EVENT_FILTER("_event_filter"),
	AGGREGATION_SELECT("_group_select"),
	AGGREGATION_FILTER("_group_filter"),
	FINAL("");

	public static final Set<ConceptCteStep> MANDATORY_STEPS = Set.of(ConceptCteStep.PREPROCESSING, ConceptCteStep.AGGREGATION_SELECT, ConceptCteStep.FINAL);

	private final String suffix;

	public static Set<ConceptCteStep> withOptionalSteps(ConceptCteStep... conceptCteStep) {
		HashSet<ConceptCteStep> steps = new HashSet<>(MANDATORY_STEPS);
		steps.addAll(Set.of(conceptCteStep));
		return steps;
	}

	@Override
	public String cteName(String conceptLabel) {
		return "concept_%s%s".formatted(conceptLabel, this.suffix);
	}
}
