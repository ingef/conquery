package com.bakdata.conquery.integration.common;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.Data;
import javax.validation.constraints.NotEmpty;

@Data
public class RequiredData {

	@NotEmpty
	@Valid
	private List<RequiredTable> tables;
	@Valid @NotNull
	private List<ResourceFile> previousQueryResults = Collections.emptyList();
	private ResourceFile idMapping;
}
