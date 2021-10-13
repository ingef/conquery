package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import java.util.OptionalInt;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;

/**
 * Tests if the preceding date is any day before the reference date.
 */
public class BeforeTemporalPrecedenceMatcher implements PrecedenceMatcher {

	@Override
	public void removePreceding(CDateSet preceding, int sample) {
		// Only consider samples that are before reference's sample event
		preceding.remove(CDateRange.atLeast(sample));
	}


	@Override
	public boolean isContained(OptionalInt reference, OptionalInt preceding) {
		if (preceding.isEmpty() || reference.isEmpty()) {
			return false;
		}

		return reference.getAsInt() > preceding.getAsInt();
	}
}
