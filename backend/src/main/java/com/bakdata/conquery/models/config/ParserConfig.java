package com.bakdata.conquery.models.config;

import javax.validation.constraints.Max;

import lombok.Data;
import lombok.Setter;

@Data
@Setter
public class ParserConfig {
	/**
	 * Minimum required double precision to switch from floats to doubles.
	 * If set to a positive value, preprocessing will select float values for columns with at least minPrecision ulp.
	 * @see com.bakdata.conquery.models.events.parser.specific.RealParser#decideType()
	 * @see Math#ulp(float)
	 */
	private double minPrecision = Double.MIN_VALUE;

	@Max(Integer.MAX_VALUE)
	private int preallocateBufferBytes = 5_000_000;
}
