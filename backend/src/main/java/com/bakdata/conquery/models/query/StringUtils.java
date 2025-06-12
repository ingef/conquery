package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.common.Range;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StringUtils {
	/**
	 * Takes a substring of the string using the bounds defined in range.
	 * Min is silently truncated to 0 and any overflow results in empty strings.
	 */
	public static String getSubstringFromRange(String string, Range.IntegerRange range) {
		if (range == null || range.isAll()) {
			return string;
		}

		int length = string.length();

		final int min;

		if (range.getMin() == null || range.getMin() < 0) {
			min = 0;
		}
		else {
			min = range.getMin();
		}

		if (min > length) {
			return "";
		}

		final int max;
		if (range.getMax() == null || range.getMax() > length) {
			max = length;
		}
		else {
			max = range.getMax();
		}

		return string.substring(min, max);
	}
}
