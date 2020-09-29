package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data @RequiredArgsConstructor
public class QueryResolveContext {
	private final DatasetId submittedDataset;
	private final DatasetRegistry datasetRegistry;
	
	public Namespace getNamespace() {
		return datasetRegistry.get(submittedDataset);
	}
}
