package com.bakdata.conquery.sql.conversion.model;

import java.math.BigDecimal;
import java.util.Map;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.MajorTypeId;

public class NumberMapUtil {

	public static final Map<MajorTypeId, Class<? extends Number>> NUMBER_MAP = Map.of(
			MajorTypeId.MONEY, BigDecimal.class,
			MajorTypeId.DECIMAL, Double.class,
			MajorTypeId.REAL, Double.class,
			MajorTypeId.INTEGER, Integer.class
	);

	public static Class<? extends Number> getType(Column column) {
		return NUMBER_MAP.get(column.getType());
	}

}
