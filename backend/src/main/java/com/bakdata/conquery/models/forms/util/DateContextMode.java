package com.bakdata.conquery.models.forms.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import c10n.C10N;
import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.internationalization.DateContextModeC10n;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;

/**
 * Specifies the tempporal resolution that should be used in the resulting
 * {@link DateContext} for grouping. It is important for this class to keep the order of the
 * Enum members.
 *
 */
@RequiredArgsConstructor
public enum DateContextMode implements DateRangeSubSampler {
	/**
	 * For returning contexts with a single {@link CDateRange} for the entire
	 * {@link FeatureGroup}.
	 */
	COMPLETE(null){
		@Override
		public List<CDateRange> subdivideRange(CDateRange range) {
			return range == null ? Collections.emptyList() : List.of(range);
		}

		@Override
		public String toString(Locale locale) {

			return C10N.get(DateContextModeC10n.class, locale).complete();
		}
	},

	/**
	 * The {@link CDateRange} contexts per {@link FeatureGroup} are subdivided into
	 * years.
	 */
	YEARS(COMPLETE){
		@Override
		public List<CDateRange> subdivideRange(CDateRange range) {

			return range == null ? ImmutableList.of() : range.getCoveredYears();
		}

		@Override
		public String toString(Locale locale) {

			return C10N.get(DateContextModeC10n.class, locale).year();
		}
	},

	/**
	 * The {@link CDateRange} contexts per {@link FeatureGroup} are subdivided into
	 * quarters.
	 */
	QUARTERS(YEARS){
		@Override
		public List<CDateRange> subdivideRange(CDateRange range) {

			return range == null ? ImmutableList.of() : range.getCoveredQuarters();
		}
		

		@Override
		public String toString(Locale locale) {

			return C10N.get(DateContextModeC10n.class, locale).quarter();
		}
	},
	
	/**
	 * The {@link CDateRange} contexts per {@link FeatureGroup} are subdivided into
	 * days.
	 */
	DAYS(QUARTERS){
		@Override
		public List<CDateRange> subdivideRange(CDateRange range) {

			return range == null ? ImmutableList.of() : range.getCoveredDays();
		}

		@Override
		public String toString(Locale locale) {

			return C10N.get(DateContextModeC10n.class, locale).day();
		}
	},

	YEARS_RELATIVE_TO_START_QUARTER_ALIGNED(COMPLETE) {
		@Override
		public List<CDateRange> subdivideRange(CDateRange range) {
			List<CDateRange> result = new ArrayList<>();
			List<CDateRange> quarters = range.getCoveredQuarters();

			int quarterCount = 1;
			int yearStart = 0;
			for (CDateRange quarter : quarters) {
				if (quarterCount%4 == 1){
					// Start a new year
					yearStart = quarter.getMinValue();
				}
				if (quarterCount%4 == 0){
					// Finish a year
					result.add(CDateRange.of(yearStart, quarter.getMaxValue()));
				}
				quarterCount++;
			}

			if(quarterCount%4 != 1) {
				// The loop did not fullfill the last year it begun
				result.add(CDateRange.of(yearStart, quarters.get(quarters.size()-1).getMaxValue()));
			}

			return result;
		}

		@Override
		public String toString(Locale locale) {
			return null;
		}
	},
	YEARS_RELATIVE_TO_END_QUARTER_ALIGNED(COMPLETE) {
		@Override
		public List<CDateRange> subdivideRange(CDateRange range) {
			List<CDateRange> result = new ArrayList<>();
			List<CDateRange> quarters = Lists.reverse(range.getCoveredQuarters());

			int quarterCount = 1;
			int yearEnd = 0;
			for (CDateRange quarter : quarters) {
				if (quarterCount%4 == 1){
					// Start a new year
					yearEnd = quarter.getMaxValue();
				}
				if (quarterCount%4 == 0){
					// Finish a year
					result.add(CDateRange.of(quarter.getMinValue(), yearEnd));
				}
				quarterCount++;
			}

			if(quarterCount%4 != 1) {
				// The loop did not fullfill the last year it begun
				result.add(CDateRange.of(quarters.get(quarters.size()-1).getMinValue(), yearEnd));
			}

			return Lists.reverse(result);
		}

		@Override
		public String toString(Locale locale) {
			return null;
		}
	};
	
	@JsonIgnore
	private final DateContextMode coarser;


	private List<DateContextMode> thisAndCoarserSubdivisions;

	
	@JsonIgnore
	public List<DateContextMode> getThisAndCoarserSubdivisions(){
		if (thisAndCoarserSubdivisions != null) {
			return thisAndCoarserSubdivisions;
		}
		List<DateContextMode> thisAndCoarser = new ArrayList<>();
		if(coarser != null) {
			thisAndCoarser.addAll(coarser.getThisAndCoarserSubdivisions());
		}
		thisAndCoarser.add(this);
		return thisAndCoarserSubdivisions = Collections.unmodifiableList(thisAndCoarser);
		
	}
}
