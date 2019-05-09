package com.bakdata.conquery.models.common.daterange;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.experimental.Wither;

@Wither
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CDateRangeEnding implements CDateRange {

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
