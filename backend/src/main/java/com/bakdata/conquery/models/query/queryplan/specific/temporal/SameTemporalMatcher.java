package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import java.util.OptionalInt;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import lombok.ToString;

/**
 * Tests if reference and preceding are on the same day.
 */
@ToString
public class SameTemporalMatcher implements PrecedenceMatcher {

	@Override
	public void removePreceding(CDateSet preceding, int sample) {
		preceding.remove(CDateRange.atLeast(sample + 1));
	}

	@Override
	public boolean isContained(OptionalInt reference, OptionalInt preceding) {
		if (preceding.isEmpty() || reference.isEmpty()) {
			return false;
		}

		return (reference.getAsInt() == preceding.getAsInt());
	}
}
