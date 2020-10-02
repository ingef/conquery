package com.bakdata.conquery.models.config;

import javax.validation.constraints.Min;

import lombok.Data;

@Data
public class ParserConfig {
	@Min(0)
	private final double minPrecision = 1e-4;
}
