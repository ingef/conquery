package com.bakdata.conquery.util;

import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.worker.DatasetRegistry;

public class ResourceUtil {

	private final DatasetRegistry namespaces;

	public ResourceUtil(DatasetRegistry namespaces) {
		this.namespaces = namespaces;
	}

	public Dataset getDataset(DatasetId id) {
		return namespaces.get(id).getStorage().getDataset();
	}

	public NamespaceStorage getStorage(DatasetId id) {
		return namespaces.get(id).getStorage();
	}

	public ManagedExecution<?> getManagedQuery(ManagedExecutionId queryId) {
		return namespaces.get(queryId.getDataset()).getQueryManager().getQuery(queryId);
	}

}
