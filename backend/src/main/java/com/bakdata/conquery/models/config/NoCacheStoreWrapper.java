package com.bakdata.conquery.models.config;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.Store;

@CPSType(id = "NO_CACHE", base = CachingStoreWrapper.class)
public class NoCacheStoreWrapper implements CachingStoreWrapper {
	@Override
	public <KEY, VALUE> Store<KEY, VALUE> wrap(Store<KEY, VALUE> store) {
		return store;
	}
}
