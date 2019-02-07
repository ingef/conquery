package com.bakdata.conquery.integration.common;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import lombok.Data;

@Data
public class RequiredData {

	@NotEmpty
	@Valid
	private RequiredTable[] tables;
	@Valid @NotNull
	private List<ResourceFile> previousQueryResults = Collections.emptyList();
}
