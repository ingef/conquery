package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import com.bakdata.conquery.sql.conversion.model.CteStep;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IntervalPackingCteStep implements CteStep {

	PREVIOUS_END("previous_end", null),
	RANGE_INDEX("range_index", PREVIOUS_END),
	INTERVAL_COMPLETE("interval_complete", RANGE_INDEX);

	private final String suffix;
	private final IntervalPackingCteStep predecessor;

}
