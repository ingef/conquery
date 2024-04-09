package com.bakdata.conquery.sql.conversion.forms;

import java.time.LocalDate;
import java.util.List;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.Selects;
import org.jooq.Condition;

interface AbsoluteStratification {

	/**
	 * Finds the date range the stratification range is bound by. It can be either bound by entity date, meaning entities validity date defines the min and max
	 * stratification date bounds. Otherwise, it is bound by forms date restriction range.
	 */
	ColumnDateRange findBounds(Range<LocalDate> formDateRestriction, Selects baseStepSelects);

	/**
	 * Generates the correctly bound stratification date range based on the provided series range and mandatory bounds. Also see
	 * {@link #findBounds(Range, Selects)}.
	 * <p>
	 * A generated series will always span over full quarters and years, for example: generate_series(2012-01-01, 2013-01-01, interval '1 year') generates
	 * <table>
	 *   <tr>
	 *     <td>2012-01-01</td>
	 *   </tr>
	 *   <tr>
	 *     <td>2013-01-01</td>
	 *   </tr>
	 * </table>
	 * The function adjusts the start and/or end dates of the series to ensure they fall within the given bounds, effectively creating an intersection between
	 * the series range and the bounds. If the bounds restrict the range (bounds.startDate > series.startDate and/or bounds.endDate < series.endDate),
	 * the start and/or end date of the series will be adjusted to align with the bounds.
	 */
	ColumnDateRange createStratificationDateRange(ColumnDateRange seriesRange, ColumnDateRange bounds);

	/**
	 * Defines the conditions we need to apply when joining a generated series set with entities IDs (and dates).
	 * When we have an entity date stratification, we only keep those stratification windows where the entity date and the stratification range overlap.
	 */
	List<Condition> stratificationTableConditions(ColumnDateRange seriesRange, ColumnDateRange bounds);

}
