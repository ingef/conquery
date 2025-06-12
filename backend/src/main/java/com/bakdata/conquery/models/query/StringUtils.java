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

		final int length = string.length();

		int min = 0;

		if (range.getMin() != null && range.getMin() > 0) {
			min = range.getMin();
		}

		if (min > length) {
			return "";
		}

		int max = length;
		if (range.getMax() != null && range.getMax() < length) {
			max = range.getMax();
		}

		return string.substring(min, max);
	}
}
