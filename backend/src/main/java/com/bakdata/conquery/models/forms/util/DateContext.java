package com.bakdata.conquery.models.forms.util;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.error.ConqueryError;
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
	 */
	@Getter
	private final CDateRange dateRange;

	/**
	 * Indicates to which group the context belongs.
	 */
	@Getter
	@Setter
	private FeatureGroup featureGroup;

	/**
	 * Indicates the relative position of the context to the event context.
	 */
	@Getter
	private Integer index = null;
	
	/**
	 * The date from which the relative context were generated.
	 */
	@Getter @Setter
	private LocalDate eventDate = null;
	
	/**
	 * Indicates under which temporal subdivision mode this instance was created.
	 */
	@Getter
	@Nullable
	private Resolution subdivisionMode;

	/**
	 * Generates a date context list of sub date ranges from the given dateRangeMask.
	 * The generation of the contexts happens for each resolution with their mapped alignment.
	 * The returned list is primarily sorted in the order of the given resolutions and secondarily by the temporal
	 * succession of the contexts, e.g.: with resolutions YEARS, QUARTERS given the list would first contain the
	 * ascending year ranges and than the quarter ranges. The alignment references always the lower bound of the
	 * dateRangeMask.
	 * @param dateRangeMask The mask in which the contexts are generated
	 * @param resolutionAndAlignment The resolutions to produce and their alignment
	 * @return A sorted list of all generated contexts
	 */
	public static List<DateContext> generateAbsoluteContexts(CDateRange dateRangeMask, List<ExportForm.ResolutionAndAlignment> resolutionAndAlignment) {
		List<DateContext> dcList = new ArrayList<>();

		for (ExportForm.ResolutionAndAlignment mode : resolutionAndAlignment) {
			Function<CDateRange, List<CDateRange>> divider = getDateRangeSubdivider(AlignmentReference.START, mode.getResolution(), mode.getAlignment());
			// Start counting index form 0 for every subdivision mode
			int index = 0;
			for (CDateRange quarterInMask : divider.apply(dateRangeMask)) {
				index++;
				DateContext dc = new DateContext(quarterInMask, FeatureGroup.OUTCOME,
					// For now there is no index for complete
					mode.getResolution().equals(Resolution.COMPLETE) ? null : index, null, mode.getResolution());
				dcList.add(dc);
			}
		}
		return dcList;
	}

	/**
	 * Factory function that produces a list of {@link CDateRange}s from a given dateRangeMask according to the given
	 * {@link AlignmentReference}, {@link Resolution} and {@link Alignment}.
	 */
	public static Function<CDateRange,List<CDateRange>> getDateRangeSubdivider(AlignmentReference alignRef, Resolution resolution, Alignment alignment){
		int alignedPerResolution = resolution.getAmountForAlignment(alignment).orElseThrow(() -> new ConqueryError.ExecutionCreationPlanDateContextError(alignment, resolution));

		if (alignedPerResolution == 1) {
			// When the alignment fits the resolution we can use the the alignment subdivision directly
			return (dateRange) -> alignment.getSubdivider().apply(dateRange);
		}

		return (dateRange) -> {
			List<CDateRange> alignedSubdivisions = alignRef.getAlignedIterationDirection(alignment.getSubdivider().apply(dateRange));

			if(alignedSubdivisions.isEmpty()){
				return alignedSubdivisions;
			}

			List<CDateRange> result = new ArrayList<>();

			int alignedSubdivisionCount = 1;
			int interestingDate = 0;
			for (CDateRange alignedSubdivision : alignedSubdivisions) {
				if (alignedSubdivisionCount % alignedPerResolution == 1) {
					// Start a new resolution-sized subdivision
					interestingDate = alignRef.getInterestingBorder(alignedSubdivision);
				}
				if (alignedSubdivisionCount % alignedPerResolution == 0) {
					// Finish a resolution-sized subdivision
					result.add(alignRef.makeMergedRange(alignedSubdivision, interestingDate));
				}
				alignedSubdivisionCount++;
			}

			if (alignedSubdivisionCount % alignedPerResolution != 1) {
				// The loop did not fullfill the resolution-sized subdivision it begun
				result.add(alignRef.makeMergedRange(alignedSubdivisions.get(alignedSubdivisions.size() - 1), interestingDate));
			}

			return alignRef.getAlignedIterationDirection(result);
		};
	}

	/**
	 * Generates a list of date contexts around an index date which belong either to the feature group (before the index)
	 * or the outcome group (after the index). The computed feature and outcome date ranges cover a date range
	 * specified by featureTime*timeUnit for the the feature range (respective outcomeTime for the outcome range).
	 * These ranges are sub divided in to the coarseness of the given resolutions.
	 * The event (a certain day) itself is expanded to a date range according to the desired alignment and the indexPlacement
	 * determines to which group it belongs.
	 */
	public static List<DateContext> generateRelativeContexts(int event, IndexPlacement indexPlacement, int featureTime, int outcomeTime, CalendarUnit timeUnit, List<ExportForm.ResolutionAndAlignment> resolutionAndAlignment) {
		if (featureTime < 1 && outcomeTime < 1) {
			throw new IllegalArgumentException("Both relative times were smaller than 1 (featureTime: " + featureTime
					+ "; outcomeTime: " + outcomeTime + ")");
		}
		List<DateContext> dcList = new ArrayList<>();

		LocalDate eventdate = CDate.toLocalDate(event);

		CDateRange featureRange = generateFeatureRange(event, indexPlacement, featureTime, timeUnit);
		CDateRange outcomeRange = generateOutcomeRange(event, indexPlacement, outcomeTime, timeUnit);


		for(ExportForm.ResolutionAndAlignment mode : resolutionAndAlignment) {
			Function<CDateRange, List<CDateRange>> featureRangeDivider = getDateRangeSubdivider(AlignmentReference.END, mode.getResolution(), mode.getAlignment());
			Function<CDateRange, List<CDateRange>> outcomeRangeDivider = getDateRangeSubdivider(AlignmentReference.START, mode.getResolution(), mode.getAlignment());

			if(featureRange != null) {

				List<CDateRange> featureRanges = featureRangeDivider.apply(featureRange);
				/*
				 *  Depending on the index placement the event date belong to the feature range , outcome range or neither. This is represented in the index.
				 *  If the index placement is BEFORE, the event date is included in the most recent feature date range, which is marked by an index of 0.
				 *  If the index placement is NEUTRAL, the event date is not included in any date range and not range index is marked with 0.
				 *  If the index placement is AFTER, the event date is included in the earliest outcome date range, which is marked by 0.
				 */
				int index = indexPlacement.equals(IndexPlacement.BEFORE) ? featureRanges.size() - 1 : featureRanges.size();
				for (CDateRange subRange : featureRanges) {
					DateContext dc = new DateContext(
							subRange,
							FeatureGroup.FEATURE,
							// For now there is no index for complete
							mode.getResolution().equals(Resolution.COMPLETE) ? null : -index,
							eventdate,
							mode.getResolution()
					);
					index--;
					dcList.add(dc);
				}
			}

			if (outcomeRange != null) {

				int index = indexPlacement.equals(IndexPlacement.AFTER) ? 0 : 1;
				for (CDateRange subRange : outcomeRangeDivider.apply(outcomeRange)) {
					DateContext dc = new DateContext(
							subRange,
							FeatureGroup.OUTCOME,
							// For now there is no index for complete
							mode.getResolution().equals(Resolution.COMPLETE) ? null : index,
							eventdate,
							mode.getResolution()
					);
					index++;
					dcList.add(dc);
				}
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
	private static CDateRange generateFeatureRange(int event, IndexPlacement indexPlacement, int featureTime, CalendarUnit timeUnit) {
		if(featureTime <= 0){
			return null;
		}
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
	private static CDateRange generateOutcomeRange(int event, IndexPlacement indexPlacement, int outcomeTime, CalendarUnit resolution) {
		if (outcomeTime <= 0) {
			return null;
		}
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
