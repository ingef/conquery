package com.bakdata.conquery.util;

import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespaces;

public class ResourceUtil {

	private final Namespaces namespaces;

	public ResourceUtil(Namespaces namespaces) {
		this.namespaces = namespaces;
	}

	public Dataset getDataset(DatasetId id) {
		return namespaces.get(id).getStorage().getDataset();
	}

	public NamespaceStorage getStorage(DatasetId id) {
		return namespaces.get(id).getStorage();
	}

	public ManagedQuery getManagedQuery(DatasetId id, ManagedQueryId queryId) {
		return namespaces.get(id).getQueryManager().getQuery(queryId);
	}

}
