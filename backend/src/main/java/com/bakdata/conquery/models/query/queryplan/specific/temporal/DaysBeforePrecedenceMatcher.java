package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import java.util.OptionalInt;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import lombok.ToString;


/**
 * Tests if the days between reference and preceding are within {@link #days}.
 */
@ToString
public class DaysBeforePrecedenceMatcher implements PrecedenceMatcher {

	private final Range.IntegerRange days;

	public DaysBeforePrecedenceMatcher(Range.IntegerRange days) {
		this.days = days;
	}

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

		return days.contains(reference.getAsInt() - preceding.getAsInt());
	}
}
