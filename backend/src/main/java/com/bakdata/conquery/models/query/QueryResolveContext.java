package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.With;

@Data @RequiredArgsConstructor
public class QueryResolveContext {
	private final Dataset submittedDataset;
	private final DatasetRegistry datasetRegistry;
	private final ConqueryConfig config;

	@With
	private final DateAggregationMode dateAggregationMode;
	
	public Namespace getNamespace() {
		return datasetRegistry.get(submittedDataset.getId());
	}
}
