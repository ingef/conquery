package com.bakdata.conquery.models.common.daterange;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.With;

@With
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CDateRangeEnding extends CDateRange {

	private final int max;
	
	@Override
	public boolean contains(int rep) {
		return rep <= max;
	}
	
	@Override
	public String toString() {
		return "-∞/" + getMax();
	}
	
	@Override
	public int getMaxValue() {
		return max;
	}

	@Override
	public int getMinValue() {
		return CDateRange.NEGATIVE_INFINITY;
	}
	
	@Override
	public boolean intersects(CDateRange other) {
		if (other == null) {
			return false;
		}

		return this.getMaxValue() >= other.getMinValue();
	}
}
