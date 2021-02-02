package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import java.util.OptionalInt;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;

/**
 * Tests if the preceding date is {@link #days} before the reference, if not it must not be present.
 */
public class DaysBeforeOrNeverPrecedenceMatcher implements PrecedenceMatcher {

	private final int days;

	public DaysBeforeOrNeverPrecedenceMatcher(int days) {
		this.days = days;
	}


	@Override
	public void removePreceding(CDateSet preceding, int sample) {
		// Only consider samples that are before reference's sample event
		preceding.remove(CDateRange.atLeast(sample));
	}


	@Override
	public boolean isContained(OptionalInt reference, OptionalInt preceding) {
		if (!reference.isPresent()) {
			return false;
		}

		// never
		if (!preceding.isPresent())
			return true;

		// days before
		if ((reference.getAsInt() - preceding.getAsInt()) > days)
			return true;

		return false;
	}
}
