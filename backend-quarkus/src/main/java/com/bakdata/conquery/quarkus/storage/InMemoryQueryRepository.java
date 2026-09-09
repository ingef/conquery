package com.bakdata.conquery.quarkus.storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.bakdata.conquery.quarkus.storage.model.StoredQuery;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@IfBuildProperty(name = "conquery.storage.backend", stringValue = "IN_MEMORY", enableIfMissing = true)
public class InMemoryQueryRepository implements QueryRepository {

	private final Map<String, StoredQuery> queriesById = new ConcurrentHashMap<>();

	@Override
	public void save(StoredQuery query) {
		queriesById.put(query.getId(), query);
	}

	@Override
	public Optional<StoredQuery> findById(String queryId) {
		return Optional.ofNullable(queriesById.get(queryId));
	}

	@Override
	public List<StoredQuery> listByDataset(String datasetId) {
		return queriesById.values().stream().filter(query -> query.getDatasetId().equals(datasetId)).toList();
	}

	@Override
	public boolean deleteById(String queryId) {
		return queriesById.remove(queryId) != null;
	}
}
