package com.bakdata.conquery.models.api.description;

/**
 * This class contains the types of filters that the front end knows.
 */
public enum FEFilterType {
	SELECT,
	MULTI_SELECT,
	BIG_MULTI_SELECT,
	INTEGER_RANGE,
	REAL_RANGE,
	NONE,
	STRING,
	MONEY_RANGE
}
