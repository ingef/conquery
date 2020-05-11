package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data @RequiredArgsConstructor
public class QueryResolveContext {
	private final DatasetId submittedDataset;
	private final Namespaces namespaces;
	
	public Namespace getNamespace() {
		return namespaces.get(submittedDataset);
	}
}
