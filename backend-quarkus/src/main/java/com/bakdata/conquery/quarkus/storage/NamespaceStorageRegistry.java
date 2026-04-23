package com.bakdata.conquery.quarkus.storage;

import java.util.List;
import java.util.Optional;

public interface NamespaceStorageRegistry {

	List<DatasetCatalogRepository.DatasetRecord> listDatasets();

	Optional<NamespaceStorage> findNamespace(String datasetId);
}
