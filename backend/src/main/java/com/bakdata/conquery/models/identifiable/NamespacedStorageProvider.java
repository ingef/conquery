package com.bakdata.conquery.models.identifiable;

import java.util.Collection;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Interface for classes that can resolve an {@link NamespacedId} to a concrete object.
 */
public interface NamespacedStorageProvider extends Injectable {

	static NamespacedStorageProvider getInjected(DeserializationContext ctxt) throws JsonMappingException {
		return (NamespacedStorageProvider) ctxt
				.findInjectableValue(NamespacedStorageProvider.class.getName(), null, null);
	}

	/**
	 * Returns the storage corresponding to the given dataset.
	 * @param datasetId the dataset to query
	 * @return The storage or null if no storage corresponds to the dataset
	 *
	 * @implNote Don't call {@link Dataset#getStorageProvider()} as it is probably not yet set.
	 */
	NamespacedStorage getStorage(DatasetId datasetId);

	Collection<DatasetId> getAllDatasetIds();
}
