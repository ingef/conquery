package com.bakdata.conquery.models.config;

import javax.validation.Valid;
import javax.validation.constraints.Min;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class PreprocessingConfig {
	@Valid
	private PreprocessingDirectories[] directories;
	@Min(1)
	private int threads = Runtime.getRuntime().availableProcessors();
	@Min(0)
	private int maximumPrintedErrors = 10;
}
