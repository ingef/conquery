package com.bakdata.conquery.apiv1.forms;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import c10n.C10N;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Specifies the tempporal resolution that should be used in the resulting
 * {@link DateContext} for grouping. It is important for this class to keep the order of the
 * Enum members.
 *
 */
public enum DateContextMode {
	/**
	 * For returning contexts with a single {@link CDateRange} for the entire
	 * {@link FeatureGroup}.
	 */
	COMPLETE{
		@Override
		public List<CDateRange> subdivideRange(CDateRange range) {
			return List.of(range);
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
	YEARS{
		@Override
		public List<CDateRange> subdivideRange(CDateRange range) {
			return range.getCoveredYears();
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
	QUARTERS{
		@Override
		public List<CDateRange> subdivideRange(CDateRange range) {
			return range.getCoveredQuarters();
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
	DAYS{
		@Override
		public List<CDateRange> subdivideRange(CDateRange range) {
			return range.getCoveredDays();
		}

		@Override
		public String toString(Locale locale) {
			return C10N.get(DateContextModeC10n.class, locale).day();
		}
	};

	private List<DateContextMode> thisAndCoarserSubdivisions;

	
	@JsonIgnore
	public List<DateContextMode> getThisAndCoarserSubdivisions(){
		if (thisAndCoarserSubdivisions != null) {
			return thisAndCoarserSubdivisions;
		}
		thisAndCoarserSubdivisions = Arrays.asList(ArrayUtils.subarray(DateContextMode.values(), 0, this.ordinal()+1));
		return thisAndCoarserSubdivisions;		
	}
	

	public List<CDateRange> subdivideRange(CDateRange range){
		throw new UnsupportedOperationException();
	}
	public String toString(Locale locale) {
		throw new UnsupportedOperationException();
	}
}
