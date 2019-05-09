package com.bakdata.conquery.models.common.daterange;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.experimental.Wither;

@Wither
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CDateRangeExactly implements CDateRange {

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
