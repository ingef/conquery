package com.bakdata.conquery.models.forms.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CalendarUnit {
	DAYS(Alignment.DAY),
	QUARTERS(Alignment.QUARTER),
	YEARS(Alignment.YEAR);

	@Getter
	private final Alignment alignment;
}
