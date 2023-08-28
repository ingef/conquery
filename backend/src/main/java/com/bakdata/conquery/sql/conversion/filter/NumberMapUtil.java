package com.bakdata.conquery.sql.conversion.filter;

import java.util.Map;

import com.bakdata.conquery.models.events.MajorTypeId;

public class NumberMapUtil {

	public static final Map<MajorTypeId, Class<? extends Number>> NUMBER_MAP = Map.of(
			MajorTypeId.DECIMAL, Double.class,
			MajorTypeId.REAL, Double.class,
			MajorTypeId.INTEGER, Integer.class
	);

}
