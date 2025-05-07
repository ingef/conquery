package com.bakdata.conquery.models.identifiable.ids;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Marker interface for {@link Id}s that are bound to a {@link com.bakdata.conquery.models.worker.Namespace}/{@link com.bakdata.conquery.models.datasets.Dataset}.
 */
public abstract non-sealed class NamespacedId<TYPE> extends Id<TYPE, NamespacedStorageProvider> {

	public static WorkerStorage assertWorkerStorage(NamespacedStorage storage) {
		if (storage instanceof WorkerStorage workerStorage) {
			return workerStorage;
		}

		throw new IllegalArgumentException("Cannot be retrieved from %s".formatted(storage));
	}

	public static NamespaceStorage assertNamespaceStorage(NamespacedStorage storage) {
		if (storage instanceof NamespaceStorage namespaceStorage) {
			return namespaceStorage;
		}

		throw new IllegalArgumentException("Cannot be retrieved from %s".formatted(storage));
	}

	@Override
	public NamespacedStorageProvider getDomain() {
		return getDataset().getNamespacedStorageProvider();
	}

	@JsonIgnore
	public abstract DatasetId getDataset();

	@Override
	public void setDomain(NamespacedStorageProvider provider) {
		getDataset().setDomain(provider);
	}

}