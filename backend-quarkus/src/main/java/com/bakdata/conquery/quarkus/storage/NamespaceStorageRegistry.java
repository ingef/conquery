package com.bakdata.conquery.quarkus.storage;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.quarkus.ids.DatasetId;

public interface NamespaceStorageRegistry {

	List<DatasetCatalogRepository.DatasetRecord> listDatasets();

	Optional<NamespaceStorage> findNamespace(DatasetId datasetId);
}
