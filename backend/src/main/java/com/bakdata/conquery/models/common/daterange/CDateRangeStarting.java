package com.bakdata.conquery.models.common.daterange;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.experimental.Wither;

@Wither
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CDateRangeStarting extends CDateRange {

	private final int min;
	
	@Override
	public boolean contains(int rep) {
		return rep >= min;
	}
	
	@Override
	public String toString() {
		return String.format("%s/+∞", getMin());
	}

	@Override
	public int getMaxValue() {
		return Integer.MAX_VALUE;
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
