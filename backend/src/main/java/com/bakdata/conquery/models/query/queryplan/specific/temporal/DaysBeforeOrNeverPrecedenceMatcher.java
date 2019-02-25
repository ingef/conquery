package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.query.concept.specific.temporal.TemporalSampler;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;

import java.util.OptionalInt;

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

		return !preceding.isPresent() || (reference.getAsInt() - preceding.getAsInt()) >= days;

	}
}
