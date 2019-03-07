package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.query.concept.specific.temporal.TemporalSampler;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;

import java.util.OptionalInt;

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
		if (!preceding.isPresent() || !reference.isPresent()) {
			return false;
		}

		return reference.getAsInt() > preceding.getAsInt();
	}
}
