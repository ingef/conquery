package com.bakdata.conquery.io.storage;

import java.io.IOException;

/**
 * Interface to unify among {@link com.bakdata.conquery.io.storage.xodus.stores.KeyIncludingStore} and {@link Store} for management only.
 */
public interface ManagedStore {
	void loadData();
	void close() throws IOException;

	void clear();
	void removeStore();
}
