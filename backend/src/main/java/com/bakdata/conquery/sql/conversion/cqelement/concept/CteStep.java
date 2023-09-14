package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.HashSet;
import java.util.Set;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CteStep {

	PREPROCESSING("_preprocessing"),
	EVENT_FILTER("_event_filter"),
	AGGREGATION_SELECT("_aggregation_select"),
	AGGREGATION_FILTER("_aggregation_filter"),
	FINAL("");
	public static final Set<CteStep> MANDATORY_STEPS = Set.of(CteStep.PREPROCESSING, CteStep.AGGREGATION_SELECT, CteStep.FINAL);

	private final String suffix;

	String suffix() {
		return this.suffix;
	}

	public static Set<CteStep> withOptionalSteps(CteStep... cteStep) {
		HashSet<CteStep> steps = new HashSet<>(MANDATORY_STEPS);
		steps.addAll(Set.of(cteStep));
		return steps;
	}

}
