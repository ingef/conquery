package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import com.bakdata.conquery.sql.conversion.model.CteStep;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum IntervalPackingCteStep implements CteStep {

	PREVIOUS_END("_previous_end", null),
	RANGE_INDEX("_range_index", PREVIOUS_END),
	INTERVAL_COMPLETE("_interval_complete", RANGE_INDEX);

	private final String suffix;
	private final IntervalPackingCteStep predecessor;

	public String cteName(String nodeLabel) {
		return "%s%s".formatted(nodeLabel, this.suffix);
	}

	@Override
	public IntervalPackingCteStep predecessor() {
		return this.predecessor;
	}

}
