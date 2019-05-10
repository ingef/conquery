package com.bakdata.conquery.models.common.daterange;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.Wither;

@Wither
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CDateRangeEnding extends CDateRange {

	private final int max;
	
	@Override
	public boolean contains(int rep) {
		return rep <= max;
	}
	
	@Override
	public String toString() {
		return String.format("-∞/%s", getMax());
	}
	
	@Override
	public int getMaxValue() {
		return max;
	}

	@Override
	public int getMinValue() {
		return Integer.MIN_VALUE;
	}
}
