package com.bakdata.conquery.io.xodus.stores;

public interface IStoreInfo {

	String getXodusName();
	Class<?> getValueType();
	Class<?> getKeyType();
}
