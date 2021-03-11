package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.With;

@Data @RequiredArgsConstructor
public class QueryResolveContext {
	private final DatasetId submittedDataset;
	private final DatasetRegistry datasetRegistry;

	@With
	private final DateAggregationMode dateAggregationMode;
	
	public Namespace getNamespace() {
		return datasetRegistry.get(submittedDataset);
	}
}
