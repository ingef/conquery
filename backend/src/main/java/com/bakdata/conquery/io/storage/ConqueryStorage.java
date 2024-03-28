package com.bakdata.conquery.io.storage;

import java.io.Closeable;
import java.io.IOException;

import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ConqueryStorage implements Closeable {

	public abstract CentralRegistry getCentralRegistry();

	/**
	 * @implSpec The order defines the order of loading. Dependencies should be modeled here.
	 * @implNote If you implement this method, please do it always from scratch and not using calls to super, it can be quite annoying.
	 */
	public abstract ImmutableList<ManagedStore> getStores();

	public abstract void openStores(ObjectMapper objectMapper);
	
	public final void loadData(){
		for (ManagedStore store : getStores()) {
			store.loadData();
		}
	}

	/**
	 * Delete the storage's contents.
	 */
	public void clear(){
		for (ManagedStore store : getStores()) {
			store.clear();
		}
	}

	/**
	 * Remove the storage.
	 */
	public final void removeStorage(){
		for (ManagedStore store : getStores()) {
			store.removeStore();
		}
	}

	/**
	 * Close the storage.
	 */
	public final void close() throws IOException {
		for (ManagedStore store : getStores()) {
			store.close();
		}
	}
}
