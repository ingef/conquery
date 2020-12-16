package com.bakdata.conquery.models.forms.util;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.function.Function;

import javax.annotation.Nullable;

import c10n.C10N;
import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.internationalization.DateContextResolutionC10n;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.error.ConqueryError;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
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
	 * Specifies whether the sub date ranges (which have the maximum length specified by the {@link Resolution})
	 * are aligned with regard to beginning or the end of a date range mask. This affects the generated sub date ranges
	 * only if the {@link Alignment} is finer than the {@link Resolution}.
	 */
	public static enum AlignmentReference {
		START(){
			@Override
			public List<CDateRange> getAlignedIterationDirection(List<CDateRange> alignedSubDivisions) {
				return alignedSubDivisions;
			}

			@Override
			public int getInterestingBorder(CDateRange daterange) {
				return daterange.getMinValue();
			}

			@Override
			public CDateRange makeMergedRange(CDateRange lastDaterange, int prioInteressingBorder) {
				return CDateRange.of(prioInteressingBorder, lastDaterange.getMaxValue());
			}
		},
		END(){
			@Override
			public List<CDateRange> getAlignedIterationDirection(List<CDateRange> alignedSubDivisions) {
				return Lists.reverse(alignedSubDivisions);
			}

			@Override
			public int getInterestingBorder(CDateRange daterange) {
				return daterange.getMaxValue();
			}

			@Override
			public CDateRange makeMergedRange(CDateRange lastDaterange, int prioInteressingBorder) {
				return CDateRange.of(lastDaterange.getMinValue(), prioInteressingBorder);
			}
		};

		public abstract List<CDateRange> getAlignedIterationDirection(List<CDateRange> alignedSubDivisions);
		public abstract int getInterestingBorder(CDateRange daterange);
		public abstract CDateRange makeMergedRange(CDateRange lastDaterange, int prioInteressingBorder);
	}

	@RequiredArgsConstructor
	/**
	 * Defines the granularity into which a given date range mask is chunked.
	 * The actual size in days depends on the chosen {@link Alignment}, e.g.:
	 * If the resolution is YEARS and it should be aligned on the actual YEAR, the resulting contexts days might vary
	 * depending on if that year was a leap year.
	 * If the alignment is DAY, then all context will have a length of 365 day, except the dateRangeMask intersects an
	 * edge context.
	 */
	public static enum Resolution {
		/**
		 * For returning contexts with a single {@link CDateRange} for the entire
		 * {@link FeatureGroup}.
		 */
		COMPLETE(null, Map.of(
				Alignment.NO_ALIGN, 1)) {
			@Override
			public String toString(Locale locale) {

				return C10N.get(DateContextResolutionC10n.class, locale).complete();
			}
		},

		/**
		 * The {@link CDateRange} contexts per {@link FeatureGroup} are subdivided into
		 * years.
		 */
		YEARS(COMPLETE, Map.of(
				Alignment.YEAR, 1,
				Alignment.QUARTER, 4,
				Alignment.DAY, 365)) {
			@Override
			public String toString(Locale locale) {

				return C10N.get(DateContextResolutionC10n.class, locale).year();
			}
		},

		/**
		 * The {@link CDateRange} contexts per {@link FeatureGroup} are subdivided into
		 * quarters.
		 */
		QUARTERS(YEARS, Map.of(
				Alignment.QUARTER, 1,
				Alignment.DAY, 90)) {
			@Override
			public String toString(Locale locale) {

				return C10N.get(DateContextResolutionC10n.class, locale).quarter();
			}
		},

		/**
		 * The {@link CDateRange} contexts per {@link FeatureGroup} are subdivided into
		 * days.
		 */
		DAYS(QUARTERS, Map.of(
				Alignment.DAY, 1)) {
			@Override
			public String toString(Locale locale) {

				return C10N.get(DateContextResolutionC10n.class, locale).day();
			}
		};


		@JsonIgnore
		private final Resolution coarser;

		/**
		 * Holds which calendar alignments are supported by this resolution and
		 * the amount of how many of such subdividions fill in this resolusion subdivision.
		 */
		@JsonIgnore
		private final Map<Alignment, Integer> compatibleAlignmentsAndAmount;


		private List<Resolution> thisAndCoarserSubdivisions;

		public abstract String toString(Locale locale);

		@JsonIgnore
		public Collection<Alignment> getSupportedAlignments(){
			return compatibleAlignmentsAndAmount.keySet();
		}

		/**
		 * Returns the amount of calendar alignment sub date ranges that would fit in to this resolution.
		 */
		@JsonIgnore
		public OptionalInt getAmountForAlignment(Alignment alignment){
			if (!this.compatibleAlignmentsAndAmount.containsKey(alignment)) {
				return OptionalInt.empty();
			}
			return OptionalInt.of(this.compatibleAlignmentsAndAmount.get(alignment));
		}

		@JsonIgnore
		public List<Resolution> getThisAndCoarserSubdivisions() {
			if (thisAndCoarserSubdivisions != null) {
				return thisAndCoarserSubdivisions;
			}
			List<Resolution> thisAndCoarser = new ArrayList<>();
			if (coarser != null) {
				thisAndCoarser.addAll(coarser.getThisAndCoarserSubdivisions());
			}
			thisAndCoarser.add(this);
			return thisAndCoarserSubdivisions = Collections.unmodifiableList(thisAndCoarser);

		}
	}

	@RequiredArgsConstructor
	/**
	 * Specifier for the alignment of {@link DateContext}s of a certain resolution.
	 * The alignment provides a method to sub divide a dateRangeMask into ranges aligned to the calendar equivalent.
	 * These sub divisions can then be merged to form the equally grain or coarser desired resolution for the
	 * {@link DateContext}s.
	 */
	public static enum Alignment {
		NO_ALIGN(List::of), // Special case for resolution == COMPLETE
		DAY(CDateRange::getCoveredDays),
		QUARTER(CDateRange::getCoveredQuarters),
		YEAR(CDateRange::getCoveredYears);

		@Getter @JsonIgnore
		private final Function<CDateRange,List<CDateRange>> subdivider;
	}

	@RequiredArgsConstructor
	public static enum CalendarUnit {
		DAYS(Alignment.DAY),
		QUARTERS(Alignment.QUARTER),
		YEARS(Alignment.YEAR);

		@Getter
		private final Alignment alignment;
	}

	/**
	 * Factory function that produces a list of {@link CDateRange}s from a given dateRangeMask according to the given
	 * {@link AlignmentReference}, {@link Resolution} and {@link Alignment}.
	 */
	public static Function<CDateRange,List<CDateRange>> getDateRangeSubdivider(AlignmentReference alignRef, Resolution resolution, Alignment alignment){
		int alignedPerResolution = resolution.getAmountForAlignment(alignment).orElseThrow(() -> new ConqueryError.ExecutionCreationPlanDateContextError(alignment, resolution));

		if (alignedPerResolution == 1) {
			// When the alignment fits the resolution we can use the the alignment subdivision directly
			return (dateRange) -> {
				return alignment.getSubdivider().apply(dateRange);
			};
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
	public static List<DateContext> generateRelativeContexts(int event, IndexPlacement indexPlacement, int featureTime,	int outcomeTime, DateContext.CalendarUnit timeUnit, List<ExportForm.ResolutionAndAlignment> resolutionAndAlignment) {
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
	private static CDateRange generateFeatureRange(int event, IndexPlacement indexPlacement, int featureTime, DateContext.CalendarUnit timeUnit) {
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
	private static CDateRange generateOutcomeRange(int event, IndexPlacement indexPlacement, int outcomeTime, DateContext.CalendarUnit resolution) {
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
