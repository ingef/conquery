package com.bakdata.conquery.models.index;

public interface Index<T extends IndexKey<? extends Index<T, V>,V>, V> {

	V put(String key, Object value);

	int size();

}
