package com.bakdata.conquery.integration.sql.dialect;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MockClock {

	private static final String FIXED_DATE = "2023-03-28";

	public static Clock get() {
		return Clock.fixed(
			LocalDate.parse(FIXED_DATE).atStartOfDay().toInstant(ZoneOffset.UTC),
			ZoneId.systemDefault());
	}


}
