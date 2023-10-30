package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import com.bakdata.conquery.sql.conversion.model.CteStep;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
enum IntervalPackingCteStep implements CteStep {

	PREVIOUS_END("previous_end"),
	RANGE_INDEX("range_index"),
	INTERVAL_COMPLETE("interval_complete");

	private final String suffix;

	public String suffix() {
		return this.suffix;
	}

}
