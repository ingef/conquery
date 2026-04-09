package com.bakdata.conquery.quarkus.storage;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.quarkus.storage.model.StoredQuery;

public interface QueryRepository {

	void save(StoredQuery query);

	Optional<StoredQuery> findById(String queryId);

	List<StoredQuery> listByDataset(String datasetId);

	boolean deleteById(String queryId);
}
