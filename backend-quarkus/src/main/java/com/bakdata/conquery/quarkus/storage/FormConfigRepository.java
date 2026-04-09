package com.bakdata.conquery.quarkus.storage;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.quarkus.storage.model.StoredFormConfig;

public interface FormConfigRepository {

	void save(StoredFormConfig config);

	Optional<StoredFormConfig> findById(String formConfigId);

	List<StoredFormConfig> listByDataset(String datasetId);

	boolean deleteById(String formConfigId);
}
