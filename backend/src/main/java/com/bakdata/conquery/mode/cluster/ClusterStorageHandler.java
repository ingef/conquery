package com.bakdata.conquery.mode.cluster;

import java.util.List;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.StorageHandler;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.stores.root.StringStore;

public class ClusterStorageHandler implements StorageHandler {

	@Override
	public List<String> lookupColumnValues(NamespaceStorage namespaceStorage, Column column) {
		return namespaceStorage.getAllImports().stream()
							   .filter(imp -> imp.getTable().equals(column.getTable()))
							   .flatMap(imp -> {
								   final ImportColumn importColumn = imp.getColumns()[column.getPosition()];
								   return ((StringStore) importColumn.getTypeDescription()).iterateValues();
							   })
							   .toList();
	}
}
