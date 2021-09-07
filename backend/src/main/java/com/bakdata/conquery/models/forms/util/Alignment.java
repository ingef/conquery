package com.bakdata.conquery.models.forms.util;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Function;

/**
 * Specifier for the alignment of {@link DateContext}s of a certain resolution.
 * The alignment provides a method to sub divide a dateRangeMask into ranges aligned to the calendar equivalent.
 * These sub divisions can then be merged to form the equally grain or coarser desired resolution for the
 * {@link DateContext}s.
 */
@RequiredArgsConstructor
public enum Alignment {
	NO_ALIGN(List::of){
		@Override
		protected Map<Resolution, Integer> getAmountPerResolution() {
			return Map.of(Resolution.COMPLETE, 1);
		}
	}, // Special case for resolution == COMPLETE
	DAY(CDateRange::getCoveredDays) {
		@Override
		protected Map<Resolution, Integer> getAmountPerResolution() {
			return Map.of(
					Resolution.YEARS, 365,
					Resolution.QUARTERS, 90,
					Resolution.DAYS, 1);
		}
	},
	QUARTER(CDateRange::getCoveredQuarters) {
		@Override
		protected Map<Resolution, Integer> getAmountPerResolution() {
			return Map.of(
					Resolution.YEARS, 4,
					Resolution.QUARTERS, 1);
		}
	},
	YEAR(CDateRange::getCoveredYears) {
		@Override
		protected Map<Resolution, Integer> getAmountPerResolution() {
			return Map.of(Resolution.YEARS, 1);
		}
	};

	@Getter
	@JsonIgnore
	private final Function<CDateRange, List<CDateRange>> subdivider;

	@JsonIgnore
	protected abstract Map<Resolution, Integer> getAmountPerResolution();

	/**
	 * Returns the amount of calendar alignment sub date ranges that would fit in to this resolution.
	 */
	@JsonIgnore
	public OptionalInt getAmountForResolution(Resolution resolution) {
		Map<Resolution, Integer> amountMap = getAmountPerResolution();
		if (amountMap.containsKey(resolution)) {
			return OptionalInt.of(amountMap.get(resolution));
		}
		return OptionalInt.empty();
	}
}

