package com.bakdata.conquery.apiv1.forms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import c10n.C10N;
import com.bakdata.conquery.internationalization.DateContextModeC10n;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;

/**
 * Specifies the tempporal resolution that should be used in the resulting
 * {@link DateContext} for grouping. It is important for this class to keep the order of the
 * Enum members.
 *
 */
@RequiredArgsConstructor
public enum DateContextMode {
	/**
	 * For returning contexts with a single {@link CDateRange} for the entire
	 * {@link FeatureGroup}.
	 */
	COMPLETE(null){
		@Override
		public List<CDateRange> subdivideRange(CDateRange range) {
			return range == null ? ImmutableList.of() : List.of(range);
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

	public abstract List<CDateRange> subdivideRange(CDateRange range);

	public abstract String toString(Locale locale);
}
