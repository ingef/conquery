package com.bakdata.conquery.quarkus.storage.xodus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.quarkus.storage.QueryRepository;
import com.bakdata.conquery.quarkus.storage.model.StoredQuery;
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
public class XodusQueryRepository implements QueryRepository {

	private static final String STORE_NAME = "quarkus_queries";

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
	public void save(StoredQuery query) {
		String payload = serialize(query);
		environment.executeInTransaction(tx -> store.put(tx, StringBinding.stringToEntry(query.getId()), StringBinding.stringToEntry(payload)));
	}

	@Override
	public Optional<StoredQuery> findById(String queryId) {
		return environment.computeInReadonlyTransaction(tx -> {
			var value = store.get(tx, StringBinding.stringToEntry(queryId));
			if (value == null) {
				return Optional.empty();
			}
			return Optional.of(deserialize(StringBinding.entryToString(value), StoredQuery.class));
		});
	}

	@Override
	public List<StoredQuery> listByDataset(String datasetId) {
		return environment.computeInReadonlyTransaction(tx -> {
			List<StoredQuery> result = new ArrayList<>();
			try (Cursor cursor = store.openCursor(tx)) {
				while (cursor.getNext()) {
					StoredQuery query = deserialize(StringBinding.entryToString(cursor.getValue()), StoredQuery.class);
					if (datasetId.equals(query.getDatasetId())) {
						result.add(query);
					}
				}
			}
			return result;
		});
	}

	@Override
	public boolean deleteById(String queryId) {
		return environment.computeInTransaction(tx -> store.delete(tx, StringBinding.stringToEntry(queryId)));
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
