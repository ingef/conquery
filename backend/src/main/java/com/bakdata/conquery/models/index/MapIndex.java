package com.bakdata.conquery.models.index;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MapIndex implements Index<MapIndexKey> {

	private final String externalTemplate;

	private final HashMap<String, String> mappings = new HashMap<>();

	@Override
	public void put(String key, Map<String, String> templateToConcrete) {
		if (mappings.containsKey(key)) {
			throw new IllegalArgumentException("The key '" + key + "' already exists in the index. Cannot map '" + key + "' -> '" + templateToConcrete + "'.");
		}
		mappings.put(key, templateToConcrete.get(externalTemplate));
	}

	@Override
	public int size() {
		return mappings.size();
	}

	@Override
	public void finalizer() {
		// Nothing to finalize
	}

	public String get(String internalValue, String defaultValue) {
		return mappings.getOrDefault(internalValue, defaultValue);
	}
}
