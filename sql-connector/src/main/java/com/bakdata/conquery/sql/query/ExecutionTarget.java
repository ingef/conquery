package com.bakdata.conquery.sql.query;

import java.util.Optional;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Selects the dataset snapshot and configured datasource used to execute a query. */
public record ExecutionTarget(
		@NotBlank String datasetId,
		@NotBlank String dataSource,
		@NotNull Optional<String> catalogRevision
) {

	@AssertTrue(message = "catalogRevision must not be blank when present")
	public boolean isCatalogRevisionValid() {
		return catalogRevision == null || catalogRevision.isEmpty() || !catalogRevision.get().isBlank();
	}

	public ExecutionTarget(String datasetId, String dataSource) {
		this(datasetId, dataSource, Optional.empty());
	}
}
