package com.bakdata.conquery.models.common.daterange;

import com.bakdata.conquery.models.common.CDate;

import lombok.EqualsAndHashCode;
import lombok.experimental.Wither;

@Wither
@EqualsAndHashCode
public class CDateRangeClosed implements CDateRange {

	private final int min;
	private final int max;
	
	/*package*/ CDateRangeClosed(int min, int max) {
		this.min = min;
		this.max = max;

		if (min > max) {
			throw new IllegalArgumentException(
				String.format("Min(%s) is not less than max(%s)", CDate.toLocalDate(min), CDate.toLocalDate(max)));
		}
		
		if (min == Integer.MIN_VALUE || max == Integer.MAX_VALUE) {
			throw new IllegalArgumentException(
				String.format("%s is not a valid closed range", this.toString()));
		}
	}
	
	@Override
	public boolean contains(int rep) {
		return rep >= getMinValue() && rep <= getMaxValue();
	}
	
	@Override
	public String toString() {
		return String.format("%s/%s", getMin(), getMax());
	}
	
	@Override
	public int getMaxValue() {
		return max;
	}

	@Override
	public int getMinValue() {
		return min;
	}
}
