package com.bakdata.conquery.sql.query;

import java.util.Objects;
import java.util.Optional;

/** Selects the dataset snapshot and configured datasource used to execute a query. */
public record ExecutionTarget(String datasetId, String dataSource, Optional<String> catalogRevision) {

	public ExecutionTarget {
		datasetId = ModelValidation.requireNonBlank(datasetId, "datasetId");
		dataSource = ModelValidation.requireNonBlank(dataSource, "dataSource");
		catalogRevision = Objects.requireNonNull(catalogRevision, "catalogRevision")
				.map(revision -> ModelValidation.requireNonBlank(revision, "catalogRevision"));
	}

	public ExecutionTarget(String datasetId, String dataSource) {
		this(datasetId, dataSource, Optional.empty());
	}
}
