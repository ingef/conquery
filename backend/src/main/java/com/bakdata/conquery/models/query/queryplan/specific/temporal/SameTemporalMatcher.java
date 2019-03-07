package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.query.concept.specific.temporal.TemporalSampler;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;

import java.util.OptionalInt;

/**
 * Tests if reference and preceding are on the same day.
 */
public class SameTemporalMatcher implements PrecedenceMatcher {

	@Override
	public void removePreceding(CDateSet preceding, int sample) {
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
