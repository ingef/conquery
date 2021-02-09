package com.bakdata.conquery.io.xodus.stores;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class SimpleStoreInfo implements IStoreInfo {

	private final String name;
	private final Class<?> keyType;
	private final Class<?> valueType;
}
