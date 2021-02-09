package com.bakdata.conquery.io.xodus.stores;

public interface IStoreInfo {

	String getName();
	Class<?> getValueType();
	Class<?> getKeyType();
}
