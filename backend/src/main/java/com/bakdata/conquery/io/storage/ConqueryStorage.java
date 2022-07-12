package com.bakdata.conquery.io.storage;

import java.io.Closeable;

import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

public interface ConqueryStorage extends Closeable {

	CentralRegistry getCentralRegistry();

	void openStores(ObjectMapper objectMapper);
	
	void loadData();

	/**
	 * Delete the storage's contents.
	 */
	void clear();

	/**
	 * Remove the storage.
	 */
	void removeStorage();
}
