package com.bakdata.conquery.io.storage.xodus.stores;

import com.bakdata.conquery.io.storage.IStoreInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class SimpleStoreInfo implements IStoreInfo {

	private final String name;
	private final Class<?> keyType;
	private final Class<?> valueType;
}
