package com.bakdata.conquery.models.config;

import java.io.File;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.exceptions.validators.ExistingFile;

import groovy.transform.ToString;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @ToString @NoArgsConstructor @AllArgsConstructor
public class PreprocessingDirectories {
	@NotNull @ExistingFile(directory=true)
	private File csv;
	@NotNull @ExistingFile(directory=true)
	private File descriptions;
	@NotNull @ExistingFile(directory=true)
	private File preprocessedOutput;
}
