package com.bakdata.conquery.sql.conversion.dialect;


import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingContext;
import com.bakdata.conquery.sql.conversion.model.QueryStep;

/**
 * Packing intervals involves packing groups of intersecting validity date intervals into their respective continuous intervals.
 * <p>
 * See <a href="https://www.itprotoday.com/sql-server/new-solution-packing-intervals-problem">Interval Packing</a>
 */
public interface IntervalPacker {

	String PREVIOUS_END_FIELD_NAME = "previous_end";
	String RANGE_INDEX_FIELD_NAME = "range_index";
	String RANGE_START_MIN_FIELD_NAME = "range_start_min";
	String RANGE_END_MAX_FIELD_NAME = "range_end_max";

	/**
	 * Depending on the dialect, one or more {@link QueryStep}s are created to aggregate multiple validity date entries of the same subject.
	 *
	 * <p>
	 * Only the last {@link QueryStep} containing the aggregated validity dates will be returned.
	 */
	QueryStep createIntervalPackingSteps(IntervalPackingContext context);

}
