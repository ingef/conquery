package com.bakdata.conquery.models.config;

import java.io.File;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.exceptions.validators.ExistingFile;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class StorageConfig {

	@ExistingFile(directory = true)
	private File directory = new File("storage");
	@ExistingFile(directory = true)
	private File preprocessedRoot;
	@NotNull @Valid
	private XodusConfig xodus = new XodusConfig();
}
