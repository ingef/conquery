package com.bakdata.conquery.models.forms.util;

import c10n.C10N;
import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.internationalization.DateContextResolutionC10n;
import com.bakdata.conquery.internationalization.Localized;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.RequiredArgsConstructor;

import java.util.*;

/**
 * Defines the granularity into which a given date range mask is chunked.
 * The actual size in days depends on the chosen {@link Alignment}, e.g.:
 * If the resolution is YEARS and it should be aligned on the actual YEAR, the resulting contexts days might vary
 * depending on if that year was a leap year.
 * If the alignment is DAY, then all context will have a length of 365 day, except the dateRangeMask intersects an
 * edge context.
 */
@RequiredArgsConstructor
public enum Resolution implements Localized {
	/**
	 * For returning contexts with a single {@link CDateRange} for the entire
	 * {@link FeatureGroup}.
	 */
	COMPLETE(null) {
		@Override
		public String toString(Locale locale) {

			return C10N.get(DateContextResolutionC10n.class, locale).complete();
		}

		@Override
		protected List<Alignment> getCompatibleAlignments() {
			return List.of(Alignment.NO_ALIGN);
		}
	},

	/**
	 * The {@link CDateRange} contexts per {@link FeatureGroup} are subdivided into
	 * years.
	 */

	YEARS(COMPLETE) {
		@Override
		public String toString(Locale locale) {

			return C10N.get(DateContextResolutionC10n.class, locale).year();
		}

		@Override
		protected List<Alignment> getCompatibleAlignments() {
			return List.of(
					Alignment.YEAR,
					Alignment.QUARTER,
					Alignment.DAY);
		}
	},

	/**
	 * The {@link CDateRange} contexts per {@link FeatureGroup} are subdivided into
	 * quarters.
	 */
	QUARTERS(YEARS) {
		@Override
		public String toString(Locale locale) {

			return C10N.get(DateContextResolutionC10n.class, locale).quarter();
		}

		@Override
		protected List<Alignment> getCompatibleAlignments() {
			return List.of(
					Alignment.QUARTER,
					Alignment.DAY);
		}
	},

	/**
	 * The {@link CDateRange} contexts per {@link FeatureGroup} are subdivided into
	 * days.
	 */
	DAYS(QUARTERS) {
		@Override
		public String toString(Locale locale) {

			return C10N.get(DateContextResolutionC10n.class, locale).day();
		}

		@Override
		protected List<Alignment> getCompatibleAlignments() {
			return List.of(Alignment.DAY);
		}
	};


	@JsonIgnore
	private final Resolution coarser;

	private List<Resolution> thisAndCoarserSubdivisions;

	public abstract String toString(Locale locale);

	/**
	 * Returns the alignments, that are compatible with this resolution.
	 *
	 * @implNote The first aligment of the returned list is considered the default (see getDefaultAlignment)
	 */
	@JsonIgnore
	protected abstract List<Alignment> getCompatibleAlignments();

	@JsonIgnore
	public boolean isAlignmentSupported(Alignment alignment) {
		return getCompatibleAlignments().contains(alignment);
	}

	@JsonIgnore
	public Alignment getDefaultAlignment() {
		// The first alignment is considered the default
		return getCompatibleAlignments().get(0);
	}

	/**
	 * Returns the amount of calendar alignment sub date ranges that would fit in to this resolution.
	 */
	@JsonIgnore
	public OptionalInt getAmountForAlignment(Alignment alignment) {
		if (!isAlignmentSupported(alignment)) {
			return OptionalInt.empty();
		}
		return alignment.getAmountForResolution(this);
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


	@CPSType(id = "RESOLUTION", base = Localized.Provider.class)
	public static class LocalizationProvider implements Localized.Provider {

		@Override
		public String apply(Object o, Locale locale) {
			return Resolution.valueOf((String)o).toString(locale);
		}
	}
}
