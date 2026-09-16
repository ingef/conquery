package com.bakdata.conquery.quarkus.storage.xodus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.quarkus.storage.FormConfigRepository;
import com.bakdata.conquery.quarkus.storage.model.StoredFormConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jetbrains.exodus.bindings.StringBinding;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;

@ApplicationScoped
@IfBuildProperty(name = "conquery.storage.backend", stringValue = "XODUS")
public class XodusFormConfigRepository implements FormConfigRepository {

	private static final String STORE_NAME = "quarkus_form_configs";

	@Inject
	XodusEnvironmentProvider environmentProvider;

	@Inject
	ObjectMapper objectMapper;

	private Environment environment;
	private Store store;

	@PostConstruct
	void init() {
		environment = environmentProvider.getEnvironment();
		store = environment.computeInTransaction(tx -> environment.openStore(STORE_NAME, StoreConfig.WITHOUT_DUPLICATES, tx));
	}

	@Override
	public void save(StoredFormConfig config) {
		String payload = serialize(config);
		environment.executeInTransaction(tx -> store.put(tx, StringBinding.stringToEntry(config.getId()), StringBinding.stringToEntry(payload)));
	}

	@Override
	public Optional<StoredFormConfig> findById(String formConfigId) {
		return environment.computeInReadonlyTransaction(tx -> {
			var value = store.get(tx, StringBinding.stringToEntry(formConfigId));
			if (value == null) {
				return Optional.empty();
			}
			return Optional.of(deserialize(StringBinding.entryToString(value), StoredFormConfig.class));
		});
	}

	@Override
	public List<StoredFormConfig> listByDataset(String datasetId) {
		return environment.computeInReadonlyTransaction(tx -> {
			List<StoredFormConfig> result = new ArrayList<>();
			try (Cursor cursor = store.openCursor(tx)) {
				while (cursor.getNext()) {
					StoredFormConfig config = deserialize(StringBinding.entryToString(cursor.getValue()), StoredFormConfig.class);
					if (datasetId.equals(config.getDatasetId())) {
						result.add(config);
					}
				}
			}
			return result;
		});
	}

	@Override
	public boolean deleteById(String formConfigId) {
		return environment.computeInTransaction(tx -> store.delete(tx, StringBinding.stringToEntry(formConfigId)));
	}

	private String serialize(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to serialize value for Xodus store " + STORE_NAME, e);
		}
	}

	private <T> T deserialize(String payload, Class<T> type) {
		try {
			return objectMapper.readValue(payload, type);
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to deserialize value from Xodus store " + STORE_NAME, e);
		}
	}
}
