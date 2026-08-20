package com.bakdata.conquery.models.index;

import java.util.Collection;
import java.util.Map;
import javax.annotation.CheckForNull;

public interface Index<V> {

	void put(String key, Map<String, String> templateToConcrete);

	int size();

	void finalizer();

	@CheckForNull
	V external(String key);

	@CheckForNull
	Collection<V> externalMultiple(String key);

}
