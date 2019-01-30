package com.bakdata.conquery.models.query.temporal;

import com.bakdata.conquery.models.common.CDateSet;

import java.util.OptionalInt;

public interface PrecedenceMatcher {
	void removePreceding(CDateSet preceding, int sample);

	/**
	 * Tests if {@code preceding} matches {@code reference} according to the implementation classes specific criteria.
	 * @param reference
	 * @param preceding
	 * @return
	 */
	boolean isContained(OptionalInt reference, OptionalInt preceding);
}
