package com.bakdata.conquery.io.storage.xodus.stores;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class StoreInfo<KEY,VALUE> {

	private final String name;
	private final Class<KEY> keyType;
	private final Class<VALUE> valueType;
}
