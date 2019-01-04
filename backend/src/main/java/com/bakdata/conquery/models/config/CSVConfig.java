package com.bakdata.conquery.models.config;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CSVConfig {
	private char escape = '\\';
	private char comment = '\0';
	private char delimeter = ',';
	@Length(min=1, max=2) @NotNull
	private String lineSeparator = "\n";
	private char quote = '"';
	@NotNull
	private Charset encoding = StandardCharsets.UTF_8;
	private boolean skipHeader = true;
}
