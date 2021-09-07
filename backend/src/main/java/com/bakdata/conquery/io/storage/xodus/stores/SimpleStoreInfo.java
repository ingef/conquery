package com.bakdata.conquery.io.storage.xodus.stores;

import com.bakdata.conquery.io.storage.IStoreInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class SimpleStoreInfo<KEY,VALUE> implements IStoreInfo<KEY,VALUE> {

	private final String name;
	private final Class<KEY> keyType;
	private final Class<VALUE> valueType;
}
