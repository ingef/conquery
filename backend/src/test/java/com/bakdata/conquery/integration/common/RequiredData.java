package com.bakdata.conquery.integration.common;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class RequiredData {

	@NotEmpty
	@Valid
	private List<RequiredTable> tables;

	private List<RequiredSecondaryIds> secondaryIds = Collections.emptyList();

	@Valid @NotNull
	private List<ResourceFile> previousQueryResults = Collections.emptyList();
	private ResourceFile idMapping;
}
