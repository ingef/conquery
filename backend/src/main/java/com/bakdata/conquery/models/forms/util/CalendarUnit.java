package com.bakdata.conquery.models.forms.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Unit that corresponds to the amounts given in a {@link com.bakdata.conquery.apiv1.forms.export_form.RelativeMode}, to
 * determine the concrete feature and outcome date ranges. These are then subdivided in smaller stratification intervals
 * based on provided {@link Resolution}s and {@link Alignment}s (see {@link DateContext}).
 */
@RequiredArgsConstructor
public enum CalendarUnit {
	DAYS(Alignment.DAY),
	QUARTERS(Alignment.QUARTER),
	YEARS(Alignment.YEAR);

	@Getter
	private final Alignment alignment;
}
