package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import com.bakdata.conquery.sql.conversion.model.CteStep;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
enum IntervalPackingCteStep implements CteStep {

	PREVIOUS_END("_previous_end"),
	RANGE_INDEX("_range_index"),
	INTERVAL_COMPLETE("_interval_complete");

	private final String suffix;

	public String cteName(String nodeLabel) {
		return "%s%s".formatted(nodeLabel, this.suffix);
	}

}
