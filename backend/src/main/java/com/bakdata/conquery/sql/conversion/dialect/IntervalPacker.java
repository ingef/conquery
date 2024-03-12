package com.bakdata.conquery.sql.conversion.dialect;


import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingContext;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;

/**
 * Packing intervals involves packing groups of intersecting validity date intervals into their respective continuous intervals.
 * <p>
 * See <a href="https://www.itprotoday.com/sql-server/new-solution-packing-intervals-problem">Interval Packing</a>
 */
public interface IntervalPacker {

	String PREVIOUS_END_FIELD_NAME = "previous_end";
	String RANGE_INDEX_FIELD_NAME = "range_index";

	/**
	 * Depending on the dialect, one or more {@link QueryStep}s are created to aggregate multiple validity date entries of the same subject. The given
	 * {@link IntervalPackingContext#getDaterange()} is set as the validity date of the returned {@link QueryStep}.
	 *
	 * <p>
	 * Only the last {@link QueryStep} containing the aggregated validity dates will be returned.
	 */
	QueryStep aggregateAsValidityDate(IntervalPackingContext context);

	/**
	 * Depending on the dialect, one or more {@link QueryStep}s are created to aggregate multiple date entries of the same subject. The given
	 * {@link IntervalPackingContext#getDaterange()} is NOT set as the validity date of the returned {@link QueryStep}, but as a {@link SqlSelect}.
	 */
	QueryStep aggregateAsArbitrarySelect(IntervalPackingContext context);

}
