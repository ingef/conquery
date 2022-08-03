package com.bakdata.conquery.models.index;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MapIndex extends HashMap<String, String> implements Index<MapIndexKey> {

	private final String externalTemplate;

	@Override
	public void put(String key, Map<String, String> templateToConcrete) {
		if (containsKey(key)) {
			throw new IllegalArgumentException("The key '" + key + "' already exists in the index. Cannot map '" + key + "' -> '" + templateToConcrete + "'.");
		}
		super.put(key, templateToConcrete.get(externalTemplate));
	}

	@Override
	public void finalizer() {
		// Nothing to finalize
	}
}
