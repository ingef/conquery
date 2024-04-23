package com.bakdata.conquery.sql.conversion.forms;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
enum Interval {

	ONE_YEAR_INTERVAL(1),
	YEAR_AS_DAYS_INTERVAL(365),
	QUARTER_INTERVAL(3),
	NINETY_DAYS_INTERVAL(90),
	ONE_DAY_INTERVAL(1);

	public static final int DAYS_PER_YEAR = Interval.YEAR_AS_DAYS_INTERVAL.getAmount();
	public static final int DAYS_PER_QUARTER = Interval.NINETY_DAYS_INTERVAL.getAmount();
	public static final int MONTHS_PER_QUARTER = Interval.QUARTER_INTERVAL.getAmount();

	private final int amount;

}
