package com.bakdata.conquery.models.index;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@ToString(onlyExplicitlyIncluded = true)
public class MapIndex implements Index<MapIndexKey> {

	@ToString.Include
	private final String externalTemplate;

	private final Map<String, List<String>> mappings = new HashMap<>();

	private final boolean allowMultiples;

	@Override
	public void put(String key, Map<String, String> templateToConcrete) {
		if (mappings.containsKey(key) && !allowMultiples) {
			throw new IllegalArgumentException("The key `%s` already exists in this index.".formatted(key));
		}

		mappings.computeIfAbsent(key, (ignored) -> new LinkedList<>())
				.add(templateToConcrete.get(externalTemplate));
	}

	@Override
	public int size() {
		return mappings.size();
	}

	@Override
	public void finalizer() {
		// Nothing to finalize
	}

	public List<String> get(String internalValue, String defaultValue) {
		return mappings.getOrDefault(internalValue, List.of(defaultValue));
	}
}
