package com.bakdata.conquery.models.config;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class PreprocessingConfig {
	@Min(1)
	private int nThreads = Runtime.getRuntime().availableProcessors();
	@Min(0)
	private int maximumPrintedErrors = 10;

	@Min(0) @Max(1)
	private double faultyLineThreshold = 0.01d;

	private ParserConfig parsers = new ParserConfig();
}
