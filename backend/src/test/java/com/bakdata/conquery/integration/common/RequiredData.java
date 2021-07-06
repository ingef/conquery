package com.bakdata.conquery.integration.common;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class RequiredData {

	@NotEmpty
	@Valid
	private List<RequiredTable> tables;

	private List<RequiredSecondaryId> secondaryIds = Collections.emptyList();

	@Valid @NotNull
	private List<ResourceFile> previousQueryResults = Collections.emptyList();

	@Valid @NotNull
	private List<JsonNode> previousQueries = Collections.emptyList(); // Is parsed as IQuery

	private ResourceFile idMapping;
}
