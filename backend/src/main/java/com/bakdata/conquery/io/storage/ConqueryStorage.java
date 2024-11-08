package com.bakdata.conquery.io.storage;

import java.io.Closeable;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

public interface ConqueryStorage extends Closeable {

	/**
	 * Initializes the internal stores.
	 * Injects this storage into the provided object mapper.
	 *
	 * @param objectMapper (optional) needed when the {@link com.bakdata.conquery.models.config.StoreFactory} deserializes objects
	 */
	void openStores(ObjectMapper objectMapper);

	default void loadData(){
		for (ManagedStore store : getStores()) {
			store.loadData();
		}
	}

	/**
	 * @implSpec The order defines the order of loading. Dependencies should be modeled here.
	 * @implNote If you implement this method, please do it always from scratch and not using calls to super, it can be quite annoying.
	 */
	ImmutableList<ManagedStore> getStores();

	/**
	 * Delete the storage's contents.
	 */
	default void clear(){
		for (ManagedStore store : getStores()) {
			store.clear();
		}
	}

	/**
	 * Remove the storage.
	 */
	default void removeStorage(){
		for (ManagedStore store : getStores()) {
			store.removeStore();
		}
	}

	/**
	 * Close the storage.
	 */
	default void close() throws IOException {
		for (ManagedStore store : getStores()) {
			store.close();
		}
	}
}
