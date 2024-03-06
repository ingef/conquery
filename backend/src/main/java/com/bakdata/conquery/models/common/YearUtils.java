package com.bakdata.conquery.models.common;

import java.time.LocalDate;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import lombok.experimental.UtilityClass;

/**
 * Utility class for year related functions.
 */
@UtilityClass
public final class YearUtils {
	/**
	 * Extracts the year from a date and creates a range covering that year.
	 * @param date A integer that represents a valid year, where 0 is year 0.
	 * @return A date range over the whole year.
	 */
	public static CDateRange toRange(int year) {
		return CDateRange.of(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
	}
}
