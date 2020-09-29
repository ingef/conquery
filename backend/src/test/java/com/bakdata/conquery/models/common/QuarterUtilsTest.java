package com.bakdata.conquery.models.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

class QuarterUtilsTest {

	@Test
	void nextQuarterAdjuster() {
		// YearMonths

		assertThat(QuarterUtils.nextQuarterAdjuster().adjustInto(YearMonth.of(2018, 1)))
				.isEqualTo(YearMonth.of(2018, 4));
		assertThat(QuarterUtils.nextQuarterAdjuster().adjustInto(YearMonth.of(2018, 2)))
				.isEqualTo(YearMonth.of(2018, 4));

		assertThat(QuarterUtils.nextQuarterAdjuster().adjustInto(YearMonth.of(2018, 4)))
				.isEqualTo(YearMonth.of(2018, 7));

		assertThat(QuarterUtils.nextQuarterAdjuster().adjustInto(YearMonth.of(2018, 6)))
				.isEqualTo(YearMonth.of(2018, 7));

		// LocalDates

		assertThat(QuarterUtils.nextQuarterAdjuster().adjustInto(LocalDate.of(2018, 6, 25)))
				.isEqualTo(LocalDate.of(2018, 7, 25));

		assertThat(QuarterUtils.nextQuarterAdjuster().adjustInto(LocalDate.of(2018, 12, 25)))
				.isEqualTo(LocalDate.of(2019, 1, 25));
	}

	@Test
	void firstMonthInQuarterAdjuster() {
		assertThat(QuarterUtils.firstMonthInQuarterAdjuster().adjustInto(LocalDate.of(2018, 12, 25)))
				.isEqualTo(LocalDate.of(2018, 10, 25));

		assertThat(QuarterUtils.firstMonthInQuarterAdjuster().adjustInto(LocalDateTime.of(2018, 2, 5, 5, 5)))
				.isEqualTo(LocalDateTime.of(2018, 1, 5, 5, 5));
	}

	@Test
	void firstDayOfQuarterAdjuster() {
		assertThat(QuarterUtils.firstDayOfQuarterAdjuster().adjustInto(LocalDate.of(2018, 12, 25)))
				.isEqualTo(LocalDate.of(2018, 10, 1));

		assertThat(QuarterUtils.firstDayOfQuarterAdjuster().adjustInto(LocalDateTime.of(2018, 2, 5, 5, 5)))
				.isEqualTo(LocalDateTime.of(2018, 1, 1, 5, 5));
	}

	@Test
	void lastDayOfQuarterAdjuster() {
		assertThat(QuarterUtils.lastDayOfQuarterAdjuster().adjustInto(LocalDate.of(2018, 12, 25)))
				.isEqualTo(LocalDate.of(2018, 12, 31));

		assertThat(QuarterUtils.lastDayOfQuarterAdjuster().adjustInto(LocalDateTime.of(2018, 2, 5, 5, 5)))
				.isEqualTo(LocalDateTime.of(2018, 3, 31, 5, 5));

	}
}