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

		final int min = Math.max(0, range.getMin());
		final int max = Math.min(string.length(), range.getMax());

		// Happens when string is shorter than min.
		if (min > max) {
			return "";
		}

		return string.substring(min, max);
	}
}
