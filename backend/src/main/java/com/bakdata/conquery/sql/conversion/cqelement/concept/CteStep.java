package com.bakdata.conquery.sql.conversion.cqelement.concept;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CteStep {

	PREPROCESSING("_preprocessing"),
	EVENT_FILTER("_event_filter"),
	GROUP_SELECT("_group_select"),
	GROUP_FILTER("_group_filter");

	private final String suffix;

	String suffix() {
		return this.suffix;
	}

}
