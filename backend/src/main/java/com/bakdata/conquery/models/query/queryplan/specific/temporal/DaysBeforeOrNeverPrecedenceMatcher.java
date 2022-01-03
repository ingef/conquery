package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import java.util.OptionalInt;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import lombok.ToString;

/**
 * Tests if the preceding date is {@link #days} before the reference, if not it must not be present.
 */
@ToString
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
		if (reference.isEmpty()) {
			return false;
		}

		if (preceding.isEmpty()) {
			return true;
		}

		// days before
		return (reference.getAsInt() - preceding.getAsInt()) > days;
	}
}
