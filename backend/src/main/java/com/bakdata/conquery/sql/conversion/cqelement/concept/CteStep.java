package com.bakdata.conquery.sql.conversion.cqelement.concept;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CteStep {

	PREPROCESSING("_preprocessing"),
	EVENT_FILTER("_event_filter"),
	AGGREGATION_SELECT("_aggregation_select"),
	AGGREGATION_FILTER("_aggregation_filter");

	private final String suffix;

	String suffix() {
		return this.suffix;
	}

}
