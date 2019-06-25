package com.bakdata.conquery.models.common;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.google.common.primitives.Ints;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class CQuarter {
	public CDateRange toRange(int value) {
		return CDateRange.of(value, getLastDay(value));
	}
	
	public int getLastDay(int firstEpoch) {
		return Ints.checkedCast(QuarterUtils.getLastDayOfQuarter(firstEpoch).toEpochDay());
	}
}