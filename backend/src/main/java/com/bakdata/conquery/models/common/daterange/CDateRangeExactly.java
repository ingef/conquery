package com.bakdata.conquery.models.common.daterange;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.Wither;

@Wither
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CDateRangeExactly extends CDateRange {

	private final int value;
	
	@Override
	public boolean contains(int rep) {
		return rep == value;
	}
	
	@Override
	public String toString() {
		return String.format("%s/%s", getMin(), getMax());
	}
	
	@Override
	public int getMaxValue() {
		return value;
	}

	@Override
	public int getMinValue() {
		return value;
	}
}
