package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import java.util.OptionalInt;

import com.bakdata.conquery.models.common.CDateSet;

/**
 * Interface for handling of TemporalQuery logic.
 */
public interface PrecedenceMatcher {

	/**
	 * Remove days before {@code sample} according to specified logic.
	 * @param preceding the set to manipulate
	 * @param sample the last {@link com.bakdata.conquery.models.common.CDate} to be included.
	 */
	void removePreceding(CDateSet preceding, int sample);

	/**
	 * Tests if {@code preceding} matches {@code reference} according to the implementation classes specific criteria.
	 * @param reference
	 * @param preceding
	 * @return
	 */
	boolean isContained(OptionalInt reference, OptionalInt preceding);
}
