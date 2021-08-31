package com.bakdata.conquery.models.forms.util;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@RequiredArgsConstructor
/**
 * Specifier for the alignment of {@link DateContext}s of a certain resolution.
 * The alignment provides a method to sub divide a dateRangeMask into ranges aligned to the calendar equivalent.
 * These sub divisions can then be merged to form the equally grain or coarser desired resolution for the
 * {@link DateContext}s.
 */
public enum Alignment {
	NO_ALIGN(List::of), // Special case for resolution == COMPLETE
	DAY(CDateRange::getCoveredDays),
	QUARTER(CDateRange::getCoveredQuarters),
	YEAR(CDateRange::getCoveredYears);

	static {
		NO_ALIGN.amountPerResolution = Map.of(Resolution.COMPLETE, 1);
		DAY.amountPerResolution = Map.of(
				Resolution.YEARS, 365,
				Resolution.QUARTERS, 90,
				Resolution.DAYS, 1);
		QUARTER.amountPerResolution = Map.of(
				Resolution.YEARS, 4,
				Resolution.QUARTERS, 1);
		YEAR.amountPerResolution = Map.of(
				Resolution.YEARS, 1);
	}

	@Getter
	@JsonIgnore
	private final Function<CDateRange, List<CDateRange>> subdivider;

	@Getter
	@JsonIgnore
	private Map<Resolution, Integer> amountPerResolution;
}
