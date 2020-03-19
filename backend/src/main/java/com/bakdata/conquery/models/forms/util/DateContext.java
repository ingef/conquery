package com.bakdata.conquery.models.forms.util;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.bakdata.conquery.apiv1.forms.DateContextMode;
import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RequiredArgsConstructor
@AllArgsConstructor
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
	private FeatureGroup featureGroup;

	/**
	 * Indicates the relative position of the context to the event context.
	 *
	 * @return The index.
	 */
	@Getter
	private Integer index = null;
	
	/**
	 * The date from which the relative context were generated.
	 * 
	 * @return The event date
	 */
	@Getter @Setter
	private LocalDate eventDate = null;
	
	/**
	 * Indicates under which temporal subdivision mode this instance was created.
	 */
	@Getter
	@Nullable
	private DateContextMode subdivisionMode;

	public static List<DateContext> generateAbsoluteContexts(CDateRange dateRangeMask, DateContextMode subdivisionMode) {
		return generateAbsoluteContexts(dateRangeMask, List.of(subdivisionMode));
	}
	
	/**
	 * Returns the date ranges that fit into a mask specified as date range, which
	 * are optional subdivided in to year-wise or quarter-wise date ranges.
	 * If a smaller subdivision mode is chosen, 
	 *
	 * @param dateRangeMask The mask that is applied onto the dates.
	 * @param subdivisionMode    The subdivision mode that defines the granularity of the
	 *                      result.
	 * @return All date ranges as wrapped into {@link DateContext} that were in the
	 *         mask.
	 */
	public static List<DateContext> generateAbsoluteContexts(CDateRange dateRangeMask, List<DateContextMode> subdivisionModes) {
		List<DateContext> dcList = new ArrayList<>();
		
		for(DateContextMode mode : subdivisionModes) {
			// Start counting index form 0 for every subdivision mode
			int index = 0;
			for (CDateRange quarterInMask : mode.subdivideRange(dateRangeMask)) {
				index++;
				DateContext dc = new DateContext(
					quarterInMask,
					FeatureGroup.OUTCOME,
					// For now there is no index for complete
					mode.equals(DateContextMode.COMPLETE)? null : index,
					null,
					mode
					);
				dcList.add(dc);
			}
		}
		return dcList;
	}

	/**
	 * Returns the date ranges that are in the specified range around the event.
	 * 
	 * @param event       The date (as days from {@link EPOCH_DAY} from which the
	 *                    relative range is calculated.
	 * @param indexPlacement  Indicates to which {@link FeatureGroup} the range of the
	 *                    event belongs.
	 * @param featureTime The number of feature timeunit ranges.
	 * @param outcomeTime The number of outcome timeunit ranges.
	 * @param resultMode
	 * @param timeUnit
	 * @return
	 */
	public static List<DateContext> generateRelativeContexts(int event, IndexPlacement indexPlacement, int featureTime,	int outcomeTime, DateContextMode timeUnit, List<DateContextMode> subdivisionModes) {
		if (featureTime < 1 || outcomeTime < 1) {
			throw new IllegalArgumentException("Relative times were smaller than 1 (featureTime: " + featureTime
					+ "; outcomeTime: " + outcomeTime + ")");
		}
		List<DateContext> dcList = new ArrayList<>();
		
		LocalDate eventdate = CDate.toLocalDate(event);

		CDateRange featureRange = generateFeatureRange(event, indexPlacement, featureTime, timeUnit);
		CDateRange outcomeRange = generateOutcomeRange(event, indexPlacement, outcomeTime, timeUnit);


		for(DateContextMode mode : subdivisionModes) {
			List<CDateRange> featureRanges = mode.subdivideRange(featureRange);
			int index = indexPlacement.equals(IndexPlacement.BEFORE) ? featureRanges.size() - 1 : featureRanges.size();
			for (CDateRange subRange : featureRanges) {
				DateContext dc = new DateContext(
					subRange,
					FeatureGroup.FEATURE,
					// For now there is no index for complete
					mode.equals(DateContextMode.COMPLETE)? null : -index,
					eventdate,
					mode
					);
				index--;
				dcList.add(dc);
			}

			index = indexPlacement.equals(IndexPlacement.AFTER) ? 0 : 1;
			for (CDateRange subRange : mode.subdivideRange(outcomeRange)) {
				DateContext dc = new DateContext(
					subRange,
					FeatureGroup.OUTCOME,
					// For now there is no index for complete
					mode.equals(DateContextMode.COMPLETE)? null : index,
					eventdate,
					mode
					);
				index++;
				dcList.add(dc);
			}			
		}
		return dcList;
	}

	/**
	 * Calculates the feature range.
	 * 
	 * @param event       The event date to which the range is relative.
	 * @param indexPlacement  Indicates to which {@link FeatureGroup} the event index
	 *                    belongs.
	 * @param featureTime the time units to be included.
	 * @param timeUnit  The time unit.
	 * @return The feature range.
	 */
	private static CDateRange generateFeatureRange(int event, IndexPlacement indexPlacement, int featureTime,
		DateContextMode timeUnit) {
		if (indexPlacement.equals(IndexPlacement.BEFORE)) {
			switch (timeUnit) {
				case DAYS:
					return CDateRange.of(event - featureTime + 1, event);
				case QUARTERS:
					LocalDate eventRangeStart = QuarterUtils
						.getFirstDayOfQuarter(CDate.toLocalDate(event).minus(featureTime - 1, IsoFields.QUARTER_YEARS));
					LocalDate eventRangeEnd = QuarterUtils.getLastDayOfQuarter(event);
					return CDateRange.of(eventRangeStart, eventRangeEnd);
				default:
					throw new IllegalArgumentException("Unsupported Resolution: " + timeUnit);
			}
		}
		// eventIndex == NEUTRAL or AFTER
		switch (timeUnit) {
			case DAYS:
				return CDateRange.of(event - featureTime, event - 1);
			case QUARTERS:
				LocalDate eventRangeStart = QuarterUtils
						.getFirstDayOfQuarter(CDate.toLocalDate(event).minus(featureTime, IsoFields.QUARTER_YEARS));
				LocalDate eventRangeEnd = QuarterUtils
						.getLastDayOfQuarter(CDate.toLocalDate(event).minus(1, IsoFields.QUARTER_YEARS));
				return CDateRange.of(eventRangeStart, eventRangeEnd);
			default:
				throw new IllegalArgumentException("Unsupported Resolution: " + timeUnit);
		}
	}

	/**
	 * Calculates the outcome range.
	 * 
	 * @param event       The event date to which the range is relative.
	 * @param indexPlacement  Indicates to which {@link FeatureGroup} the event index
	 *                    belongs.
	 * @param outcomeTime the time units to be included.
	 * @param resolution  The time unit.
	 * @return The outcome range.
	 */
	private static CDateRange generateOutcomeRange(int event, IndexPlacement indexPlacement, int outcomeTime,
		DateContextMode resolution) {
		switch(indexPlacement) {
			case AFTER:
				switch (resolution) {
					case DAYS:
						return CDateRange.of(event, event + outcomeTime - 1);
					case QUARTERS:
						LocalDate eventRangeStart = QuarterUtils.getFirstDayOfQuarter(event);
						LocalDate eventRangeEnd = QuarterUtils.getLastDayOfQuarter(
							LocalDate.ofEpochDay(event).plus(outcomeTime - 1, IsoFields.QUARTER_YEARS));
						return CDateRange.of(eventRangeStart, eventRangeEnd);
					default:
						throw new IllegalArgumentException("Unsupported Resolution: " + resolution);
				}
				// Not reachable

			case BEFORE:
			case NEUTRAL:
				switch (resolution) {
					case DAYS:
						return CDateRange.of(event + 1, event + outcomeTime);
					case QUARTERS:
						LocalDate eventRangeStart = QuarterUtils
						.getFirstDayOfQuarter(CDate.toLocalDate(event).plus(1, IsoFields.QUARTER_YEARS));
						LocalDate eventRangeEnd = QuarterUtils
							.getLastDayOfQuarter(CDate.toLocalDate(event).plus(outcomeTime, IsoFields.QUARTER_YEARS));
						return CDateRange.of(eventRangeStart, eventRangeEnd);
					default:
						throw new IllegalArgumentException("Unsupported Resolution: " + resolution);
				}
				// Not reachable

			default:
				throw new IllegalArgumentException("Unsupported index placement: " + indexPlacement);
			
		}
		
	}
}
