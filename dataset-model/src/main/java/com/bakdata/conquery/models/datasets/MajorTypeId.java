package com.bakdata.conquery.models.datasets;

import java.util.EnumSet;
import java.util.Set;

public enum MajorTypeId {

	STRING,
	INTEGER,
	BOOLEAN,
	REAL,
	DECIMAL,
	MONEY,
	DATE,
	DATE_RANGE;

	public static final Set<MajorTypeId> DATE_COMPATIBLE = EnumSet.of(DATE, DATE_RANGE);
	public static final Set<MajorTypeId> NUMERIC = EnumSet.of(INTEGER, REAL, DECIMAL, MONEY);

	public boolean isNumeric() {
		return NUMERIC.contains(this);
	}

	public boolean isDateCompatible() {
		return DATE_COMPATIBLE.contains(this);
	}
}
