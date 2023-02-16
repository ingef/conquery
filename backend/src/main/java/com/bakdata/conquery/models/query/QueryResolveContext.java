package com.bakdata.conquery.models.query;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.worker.Namespace;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.With;

@Data
@RequiredArgsConstructor
public class QueryResolveContext {
	private final Dataset dataset;
	private final Namespace namespace;
	private final ConqueryConfig config;
	private final MetaStorage storage;

	@With
	private final DateAggregationMode dateAggregationMode;

	public Dataset getDataset() {
		return namespace.getDataset();
	}
}
