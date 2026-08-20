package com.bakdata.conquery.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import lombok.Data;

@Data
public class TestNamespacedStorageProvider implements NamespacedStorageProvider {

	private final Collection<NamespacedStorage> storages = new ArrayList<>();

	public TestNamespacedStorageProvider(NamespacedStorage... contents) {
		Collections.addAll(storages, contents);
	}

	@Override
	public NamespacedStorage getStorage(DatasetId datasetId) {
		for (NamespacedStorage storage : storages) {
			if (storage.getDataset().getId().equals(datasetId)) {
				return storage;
			}
		}

		return null;
	}

	@Override
	public Collection<DatasetId> getAllDatasetIds() {
		return storages.stream().map(str -> str.getDataset().getId()).toList();
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(NamespacedStorageProvider.class, this);
	}
}
