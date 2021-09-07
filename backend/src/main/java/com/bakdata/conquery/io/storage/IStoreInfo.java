package com.bakdata.conquery.io.storage;

public interface IStoreInfo<KEY,VALUE> {

	String getName();
	Class<KEY> getKeyType();
	Class<VALUE> getValueType();
}
