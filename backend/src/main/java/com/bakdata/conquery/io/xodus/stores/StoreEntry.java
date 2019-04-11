package com.bakdata.conquery.io.xodus.stores;

import lombok.Data;

@Data
public class StoreEntry <KEY, VALUE> {
	private KEY key;
	private VALUE value;
	private long byteSize;
}
