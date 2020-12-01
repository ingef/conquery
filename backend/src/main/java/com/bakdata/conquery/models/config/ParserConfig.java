package com.bakdata.conquery.models.config;

import lombok.Data;
import lombok.Setter;

@Data
@Setter
public class ParserConfig {
	/**
	 * Minimum required double precision to switch from floats to doubles.
	 * If set to a positive value, preprocessing will select float values for columns with at least minPrecision ulp.
	 * @see com.bakdata.conquery.models.events.stores.types.parser.specific.RealParser#decideType()
	 * @see Math#ulp(float)
	 */
	private double minPrecision = Double.MIN_VALUE;
}
