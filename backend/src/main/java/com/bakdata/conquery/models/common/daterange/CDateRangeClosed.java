package com.bakdata.conquery.models.common.daterange;

import com.bakdata.conquery.models.common.CDate;
import lombok.With;

@With
public class CDateRangeClosed extends CDateRange {

	private final int min;
	private final int max;

	/*package*/ CDateRangeClosed(int min, int max) {
		this.min = min;
		this.max = max;

		if (min > max) {
			throw new IllegalArgumentException(
					String.format("Min(%s) is not less than max(%s)", CDate.toLocalDate(min), CDate.toLocalDate(max)));
		}
	}

	@Override
	public boolean contains(int rep) {
		return rep >= getMinValue() && rep <= getMaxValue();
	}

	@Override
	public int getMinValue() {
		return min;
	}

	@Override
	public int getMaxValue() {
		return max;
	}

	@Override
	public String toString() {
		if (isAll()) {
			return "-∞/+∞";
		}

		if (isAtLeast()) {
			return "-∞/" + getMax();
		}

		if (isAtMost()) {
			return getMin() + "/+∞";
		}

		return getMin() + "/" + getMax();
	}
}
