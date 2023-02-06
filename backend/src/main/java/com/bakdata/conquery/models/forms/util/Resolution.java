package com.bakdata.conquery.models.forms.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;

import c10n.C10N;
import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.internationalization.DateContextResolutionC10n;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.query.PrintSettings;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.RequiredArgsConstructor;

/**
 * Defines the granularity into which a given date range mask is chunked.
 * The actual size in days depends on the chosen {@link Alignment}, e.g.:
 * If the resolution is YEARS and it should be aligned on the actual YEAR, the resulting contexts days might vary
 * depending on if that year was a leap year.
 * If the alignment is DAY, then all context will have a length of 365 day, except the dateRangeMask intersects an
 * edge context.
 */
@RequiredArgsConstructor
public enum Resolution {
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
					Alignment.DAY
			);
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
					Alignment.DAY
			);
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

	public static String localizeValue(Object value, PrintSettings cfg) {
		if (value instanceof Resolution) {
			return ((Resolution) value).toString(cfg.getLocale());
		}
		try {
			// If the object was parsed as a simple string, try to convert it to a
			// DateContextMode to get Internationalization
			return Resolution.valueOf(value.toString()).toString(cfg.getLocale());
		}
		catch (Exception e) {
			throw new IllegalArgumentException(value + " is not a valid resolution.", e);
		}
	}
}
