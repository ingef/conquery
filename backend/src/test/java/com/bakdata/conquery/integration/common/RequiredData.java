package com.bakdata.conquery.integration.common;

import java.io.File;

import javax.validation.Valid;

import org.hibernate.validator.constraints.NotEmpty;

import lombok.Data;

@Data
public class RequiredData {

	@NotEmpty
	@Valid
	private RequiredTable[] tables;
	@Valid
	private File previousQueryResults;
}
