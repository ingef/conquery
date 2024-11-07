package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for classes that can resolve an {@link NamespacedId} to a concrete object.
 */
public interface NamespacedStorageProvider extends Injectable {

	static NamespacedStorageProvider getResolver(DeserializationContext ctxt) throws JsonMappingException {
		return (NamespacedStorageProvider) ctxt
				.findInjectableValue(NamespacedStorageProvider.class.getName(), null, null);
	}

	/**
	 * Almost identical to {@link NamespacedStorageProvider#getStorage(DatasetId)}, but throws an {@link IllegalArgumentException} if no storage could be resolved.
	 * @return the storage or throws an {@link IllegalArgumentException} if the storage could not be resolved.
	 */
	@NotNull
	default NamespacedStorage resolveStorage(DatasetId datasetId) {
		NamespacedStorage storage = getStorage(datasetId);
		if (storage == null) {
			throw new IllegalArgumentException("Unknown dataset: %s".formatted(datasetId));
		}
		return storage;
	}

	/**
	 * Returns the storage corresponding to the given dataset.
	 * @param datasetId the dataset to query
	 * @return The storage or null if no storage corresponds to the dataset
	 *
	 * @implNote Don't call {@link Dataset#getNamespacedStorageProvider()} as it is probably not yet set.
	 */
	NamespacedStorage getStorage(DatasetId datasetId);
}
