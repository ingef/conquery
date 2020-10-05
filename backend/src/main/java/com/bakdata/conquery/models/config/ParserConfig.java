package com.bakdata.conquery.models.config;

import lombok.Data;

@Data
public class ParserConfig {
	/**
	 * Minimum required double precision to switch from floats to doubles.
	 * If set to a positive value, preprocessing will select float values for columns with at least minPrecision ulp.
	 * @see com.bakdata.conquery.models.types.parser.specific.RealParser#decideType()
	 * @see Math#ulp(float)
	 */
	private final double minPrecision = Double.MIN_VALUE;
}
