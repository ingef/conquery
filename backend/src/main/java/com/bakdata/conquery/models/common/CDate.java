package com.bakdata.conquery.models.common;

import java.time.LocalDate;
import java.time.YearMonth;

import com.google.common.primitives.Ints;
import lombok.experimental.UtilityClass;

/**
 * Helper class to convert between {@link LocalDate} and {@link LocalDate#toEpochDay()}.
 *
 * Internal representation of Conquery is always based on epoch-days, but if the query-engine requires they should use {@link CDate} to resolve the proper value.
 */
@UtilityClass
public final class CDate {

	public int ofLocalDate(LocalDate date) {
		return Ints.checkedCast(date.toEpochDay());
	}
	
	public int ofLocalDate(LocalDate date, int defaultValue) {
		if(date == null) {
			return defaultValue;
		}
		return Ints.checkedCast(date.toEpochDay());
	}

	public LocalDate toLocalDate(int date) {
		return LocalDate.ofEpochDay(date);
	}

	public static boolean isFirstDayOfMonth(LocalDate date) {
		return date.getDayOfMonth() == 1;
	}

	public static boolean isLastDayOfMonth(LocalDate date) {
		return date.getDayOfMonth() == YearMonth.from(date).lengthOfMonth();
	}

	public boolean isPositiveInfinity(int epochDay) {
		return epochDay == Integer.MAX_VALUE;
	}

	public boolean isNegativeInfinity(int epochDay) {
		return epochDay == Integer.MIN_VALUE;
	}

}
