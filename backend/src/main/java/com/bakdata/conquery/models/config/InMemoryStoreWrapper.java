package com.bakdata.conquery.models.config;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.Store;
import com.bakdata.conquery.io.storage.StoreMappings;

@CPSType(id = "IN_MEMORY", base = CachingStoreWrapper.class)
public class InMemoryStoreWrapper implements CachingStoreWrapper {
	@Override
	public <KEY, VALUE> Store<KEY, VALUE> wrap(Store<KEY, VALUE> store) {
		return StoreMappings.inMemory(store);
	}
}
