package com.bakdata.conquery.models.index;

import java.util.Collection;
import java.util.Map;

public interface Index<V> {

	void put(String key, Map<String, String> templateToConcrete);

	int size();

	void finalizer();

	V external(String key, V defaultValue);

	Collection<V> externalMultiple(String key, V defaultValue);

}
