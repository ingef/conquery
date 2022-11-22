package com.bakdata.conquery.models.common.daterange;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.With;

@With
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CDateRangeStarting extends CDateRange {

	private final int min;
	
	@Override
	public boolean contains(int rep) {
		return rep >= min;
	}
	
	@Override
	public String toString() {
		return getMin() + "/+∞";
	}

	@Override
	public int getMaxValue() {
		return CDateRange.MAX_VALUE;
	}

	@Override
	public int getMinValue() {
		return min;
	}
	
	@Override
	public boolean intersects(CDateRange other) {
		if (other == null) {
			return false;
		}

		return this.getMinValue() <= other.getMaxValue();
	}
}
