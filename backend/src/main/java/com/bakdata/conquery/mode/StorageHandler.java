package com.bakdata.conquery.mode;

import java.util.stream.Stream;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Column;

public interface StorageHandler {

	Stream<String> lookupColumnValues(NamespaceStorage namespaceStorage, Column column);

}
