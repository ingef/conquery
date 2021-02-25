package com.bakdata.conquery.io.storage;

public interface IStoreInfo {

	String getName();
	Class<?> getValueType();
	Class<?> getKeyType();
}
