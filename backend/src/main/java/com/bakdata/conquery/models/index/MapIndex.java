package com.bakdata.conquery.models.index;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MapIndex implements Index<MapIndexKey> {

	private final String externalTemplate;
	private final HashMap<String, Set<String>> delegate = new HashMap<>();

	@Override
	public void put(String key, Map<String, String> templateToConcrete) {
		if (containsKey(key)) {
			throw new IllegalArgumentException("The key '%s' already exists in the index. Cannot map '%s' -> '%s'.".formatted(key, key, templateToConcrete));
		}
		delegate.computeIfAbsent(key, (ignored) -> new HashSet<>())
				.add(templateToConcrete.get(externalTemplate));
	}

	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public void finalizer() {
		// Nothing to finalize
	}

	public Collection<String> get(Object key, String defaultValue) {
		return delegate.getOrDefault(key, Set.of(defaultValue));
	}
}
