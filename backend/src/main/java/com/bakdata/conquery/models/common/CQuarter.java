package com.bakdata.conquery.models.common;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.google.common.math.IntMath;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class CQuarter {
	public CDateRange toRange(int value) {
		return CDateRange.of(value, getLastDay(value));
	}
	
	public int getLastDay(int firstEpoch) {
		int withoutLeaps = firstEpoch - (firstEpoch+730)/(4*365);
		switch(IntMath.mod(withoutLeaps, 365)) {
			//Q1
			case 0:
				return firstEpoch+89;
			//Q1 leap year / Q2
			case 364:
			case 90:
				return firstEpoch+90;
			//Q3/4
			case 181:
			case 273:
				return firstEpoch+91;
			default:
				throw new IllegalStateException("Unknown day "+IntMath.mod(withoutLeaps, 365));
		}
	}
}