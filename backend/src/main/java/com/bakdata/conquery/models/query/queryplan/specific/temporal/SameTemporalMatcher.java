package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import java.util.OptionalInt;

import com.bakdata.conquery.models.common.ICDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;

/**
 * Tests if reference and preceding are on the same day.
 */
public class SameTemporalMatcher implements PrecedenceMatcher {

	@Override
	public void removePreceding(ICDateSet preceding, int sample) {
		preceding.remove(CDateRange.atLeast(sample + 1));
	}

	@Override
	public boolean isContained(OptionalInt reference, OptionalInt preceding) {
		if (!preceding.isPresent() || !reference.isPresent()) {
			return false;
		}

		return (reference.getAsInt() == preceding.getAsInt());
	}
}
