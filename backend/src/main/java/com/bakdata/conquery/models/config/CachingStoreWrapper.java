package com.bakdata.conquery.models.config;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.storage.Store;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@CPSBase
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public interface CachingStoreWrapper {

	<KEY, VALUE> Store<KEY, VALUE> wrap(Store<KEY, VALUE> store);
}
