package com.bakdata.conquery.models.common.daterange;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.With;

@With
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CDateRangeOpen extends CDateRange {
	public static final CDateRange INSTANCE = new CDateRangeOpen();

	@Override
	public boolean contains(int rep) {
		return true;
	}
	
	@Override
	public String toString() {
		return "-∞/+∞";
	}
	
	@Override
	public int getMaxValue() {
		return Integer.MAX_VALUE;
	}

	@Override
	public int getMinValue() {
		return Integer.MIN_VALUE;
	}
}
