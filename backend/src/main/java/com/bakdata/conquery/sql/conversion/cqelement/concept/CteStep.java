package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Set;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CteStep {

	PREPROCESSING("_preprocessing"),
	EVENT_FILTER("_event_filter"),
	AGGREGATION_SELECT("_aggregation_select"),
	AGGREGATION_FILTER("_aggregation_filter"),
	FINAL("");

	private final String suffix;

	String suffix() {
		return this.suffix;
	}

	public static Set<CteStep> mandatorySteps() {
		return Set.of(CteStep.PREPROCESSING, CteStep.AGGREGATION_SELECT, CteStep.FINAL);
	}

	;

}
