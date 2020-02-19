package com.bakdata.conquery.models.forms.util;

import com.bakdata.conquery.models.common.daterange.CDateRange;

/**
 * Specifies the smallest time unit that should be used in the resulting
 * {@link DateContext} for grouping.
 *
 */
public enum DateContextMode {
	/**
	 * For returning contexts with a single {@link CDateRange} for the entire
	 * {@link FeatureGroup}.
	 */
	COMPLETE_ONLY,

	/**
	 * The {@link CDateRange} contexts per {@link FeatureGroup} are subdivided into
	 * years.
	 */
	YEAR_WISE,

	/**
	 * The {@link CDateRange} contexts per {@link FeatureGroup} are subdivided into
	 * quarters.
	 */
	QUARTER_WISE;
}
