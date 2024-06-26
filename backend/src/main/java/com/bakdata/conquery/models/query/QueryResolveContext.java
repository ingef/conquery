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
	private final Namespace namespace;
	private final ConqueryConfig config;
	private final MetaStorage storage;

	// TODO Remove this. It influences how many columns a result has (NONE-Mode vs. others)
	// Because of this we need to resolve a query first in order to obtain a FullExecutionStatus
	@With
	private final DateAggregationMode dateAggregationMode;

	public Dataset getDataset() {
		return namespace.getDataset();
	}
}
