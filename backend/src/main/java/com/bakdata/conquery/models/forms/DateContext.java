package com.bakdata.conquery.models.forms;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.CDateSet;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RequiredArgsConstructor
@EqualsAndHashCode
@ToString
public class DateContext {

	/**
	 * The date range.
	 *
	 * @return The date range
	 */
	@Getter
	private final CDateRange dateRange;

	/**
	 * Indicates to which group the context belongs.
	 *
	 * @return The groups.
	 */
	@Getter
	@Setter
	private EnumSet<FeatureGroup> featureGroups;

	/**
	 * Returns the date ranges that fit into a mask specified as date range, which
	 * are optional subdivided in to year-wise or quarter-wise date ranges.
	 *
	 * @param dates
	 *            The event dates which are processed.
	 * @param dateRangeMask
	 *            The mask that is applied onto the dates.
	 * @param resultMode
	 *            The subdivision mode that defines the granularity of the result.
	 * @return All date ranges as wrapped into {@link DateContext} that were in the
	 *         mask.
	 */
	public static List<DateContext> generateAbsoluteContexts(CDateSet dates, CDateRange dateRangeMask, DateContextMode resultMode) {
		List<DateContext> dcList = new ArrayList<>();

		if (resultMode == DateContextMode.COMPLETE) {
			for (CDateRange entryDateRange : dates.asRanges()) {
				if (dateRangeMask.intersects(entryDateRange)) {
					DateContext dc = new DateContext(dateRangeMask);
					dc.setFeatureGroups(EnumSet.of(FeatureGroup.OUTCOME));
					dcList.add(dc);
				}
			}
		}

		// Handle years
		if (resultMode == DateContextMode.YEAR_WISE) {
			List<CDateRange> maskYears = dateRangeMask.getCoveredYears();
			for (CDateRange entryDateRange : dates.asRanges()) {
				for (CDateRange yearInMask : maskYears) {
					if (yearInMask.intersects(entryDateRange)) {
						DateContext dc = new DateContext(yearInMask);
						dc.setFeatureGroups(EnumSet.of(FeatureGroup.OUTCOME));
						dcList.add(dc);
					}
				}
			}
		}

		// Handle quarters
		if (resultMode == DateContextMode.QUARTER_WISE) {
			List<CDateRange> maskQuarters = dateRangeMask.getCoveredQuarters();
			for (CDateRange entryDateRange : dates.asRanges()) {
				for (CDateRange quarterInMask : maskQuarters) {
					if (quarterInMask.intersects(entryDateRange)) {
						DateContext dc = new DateContext(quarterInMask);
						dc.setFeatureGroups(EnumSet.of(FeatureGroup.OUTCOME));
						dcList.add(dc);
					}
				}
			}
		}

		return dcList;
	}
}
