package com.bakdata.conquery.models.common;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;

@UtilityClass
public final class QuarterUtils {

	private static final TemporalAdjuster FIRST_MONTH_IN_QUARTER_ADJUSTER =
			temporal -> temporal.with(ChronoField.MONTH_OF_YEAR, getFirstMonthOfQuarter(temporal.get(IsoFields.QUARTER_OF_YEAR)).getValue());

	private static final TemporalAdjuster FIRST_DAY_OF_QUARTER_ADJUSTER =
			temporal -> TemporalAdjusters.firstDayOfMonth().adjustInto(firstMonthInQuarterAdjuster().adjustInto(temporal));

	private static final TemporalAdjuster LAST_DAY_OF_QUARTER_ADJUSTER =
			temporal -> (TemporalAdjusters.firstDayOfMonth().adjustInto(nextQuarterAdjuster().adjustInto(temporal))).minus(1, ChronoUnit.DAYS);


	public static boolean isFirstMonthOfQuarter(LocalDate date) {
		Month month = date.getMonth();
		return month == month.firstMonthOfQuarter();
	}

	public static YearMonth getYearQuarter(int year, int quarter) {
		return YearMonth.from(getFirstDayOfQuarter(year, quarter));
	}

	public static boolean isBeginOfQuarter(LocalDate date) {
		return CDate.isFirstDayOfMonth(date) && isFirstMonthOfQuarter(date);
	}

	public static boolean isEndOfQuarter(LocalDate date) {
		return isBeginOfQuarter(date.plusDays(1));
	}

	public static Month getFirstMonthOfQuarter(int quarter) {
		switch (quarter) {
			case 1:
				return Month.JANUARY;
			case 2:
				return Month.APRIL;
			case 3:
				return Month.JULY;
			case 4:
				return Month.OCTOBER;
			default:
				throw new IllegalStateException(String.format("Quarter %d exceeds 1-4", quarter));
		}
	}

	public static LocalDate getFirstDayOfQuarter(int year, int quarter) {
		Month month = getFirstMonthOfQuarter(quarter);
		return LocalDate.of(year, month, 1);
	}

	public static LocalDate getLastDayOfQuarter(int year, int quarter) {
		return (LocalDate) lastDayOfQuarterAdjuster().adjustInto(getFirstDayOfQuarter(year, quarter));
	}

	public static CDateRange fromQuarter(int year, int quarter) {
		LocalDate start = getFirstDayOfQuarter(year, quarter);
		LocalDate end = getLastDayOfQuarter(year, quarter);
		return CDateRange.of(start, end);
	}

	public static TemporalAdjuster nextQuarterAdjuster() {
		return temporal -> {
			if (temporal.get(IsoFields.QUARTER_OF_YEAR) == 4) {
				return temporal.with(ChronoField.MONTH_OF_YEAR, 1)
					.with(ChronoField.YEAR, (temporal).get(ChronoField.YEAR) + 1);
			}
			else {
				return temporal.with(ChronoField.MONTH_OF_YEAR, getFirstMonthOfQuarter(temporal.get(IsoFields.QUARTER_OF_YEAR) + 1).getValue());
			}
		};
	}

	public static TemporalAdjuster firstMonthInQuarterAdjuster() {
		return FIRST_MONTH_IN_QUARTER_ADJUSTER;
	}

	public static TemporalAdjuster firstDayOfQuarterAdjuster() {
		return FIRST_DAY_OF_QUARTER_ADJUSTER;
	}

	public static TemporalAdjuster lastDayOfQuarterAdjuster() {
		return LAST_DAY_OF_QUARTER_ADJUSTER;
	}
}
