package com.bakdata.conquery.mode.cluster;

import java.util.stream.Stream;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.StorageHandler;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.stores.root.StringStore;

public class ClusterStorageHandler implements StorageHandler {

	@Override
	public Stream<String> lookupColumnValues(NamespaceStorage namespaceStorage, Column column) {
		return namespaceStorage.getAllImports().stream()
							   .filter(imp -> imp.getTable().equals(column.getTable()))
							   .flatMap(imp -> {
								   final ImportColumn importColumn = imp.getColumns()[column.getPosition()];
								   return ((StringStore) importColumn.getTypeDescription()).iterateValues();
							   });
	}
}
