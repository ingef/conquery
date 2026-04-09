package com.bakdata.conquery.quarkus.storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.bakdata.conquery.quarkus.storage.model.StoredFormConfig;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InMemoryFormConfigRepository implements FormConfigRepository {

	private final Map<String, StoredFormConfig> configsById = new ConcurrentHashMap<>();

	@Override
	public void save(StoredFormConfig config) {
		configsById.put(config.id(), config);
	}

	@Override
	public Optional<StoredFormConfig> findById(String formConfigId) {
		return Optional.ofNullable(configsById.get(formConfigId));
	}

	@Override
	public List<StoredFormConfig> listByDataset(String datasetId) {
		return configsById.values().stream().filter(config -> config.datasetId().equals(datasetId)).toList();
	}

	@Override
	public boolean deleteById(String formConfigId) {
		return configsById.remove(formConfigId) != null;
	}
}
