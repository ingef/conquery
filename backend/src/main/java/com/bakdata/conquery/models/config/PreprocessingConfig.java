package com.bakdata.conquery.models.config;

import javax.validation.Valid;
import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotEmpty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class PreprocessingConfig {
	@NotEmpty @Valid
	private PreprocessingDirectories[] directories;
	@Min(1)
	private int threads = Runtime.getRuntime().availableProcessors();
}
