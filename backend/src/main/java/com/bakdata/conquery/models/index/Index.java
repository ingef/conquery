package com.bakdata.conquery.models.index;

import java.util.Map;

public interface Index<T extends IndexKey<? extends Index<T>>> {

	void put(String key, Map<String, String> templateToConcrete);

	int size();

	void finalizer();

}
