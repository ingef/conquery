package com.bakdata.conquery.mode;

import java.util.List;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Column;

public interface StorageHandler {

	List<String> lookupColumnValues(NamespaceStorage namespaceStorage, Column column);

}
