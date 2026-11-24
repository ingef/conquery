package com.bakdata.conquery.models.forms.util;

import java.util.OptionalInt;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** Specifies how ranges of a given {@link Resolution} are aligned to calendar boundaries. */
public enum Alignment {
	NO_ALIGN,
	DAY,
	QUARTER,
	YEAR;

	/** Returns the number of aligned ranges that fit into the given resolution. */
	@JsonIgnore
	public OptionalInt getAmountForResolution(Resolution resolution) {
		final int amount = switch (this) {
			case NO_ALIGN -> resolution == Resolution.COMPLETE ? 1 : 0;
			case DAY -> switch (resolution) {
				case YEARS -> 365;
				case QUARTERS -> 90;
				case DAYS -> 1;
				default -> 0;
			};
			case QUARTER -> switch (resolution) {
				case YEARS -> 4;
				case QUARTERS -> 1;
				default -> 0;
			};
			case YEAR -> resolution == Resolution.YEARS ? 1 : 0;
		};
		return amount == 0 ? OptionalInt.empty() : OptionalInt.of(amount);
	}
}
