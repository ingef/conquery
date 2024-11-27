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
public class MapMultiIndex implements Index<String> {

	private final String externalTemplate;
	private final Map<String, Set<String>> delegate = new HashMap<>();

	@Override
	public void put(String key, Map<String, String> templateToConcrete) {
		delegate.computeIfAbsent(key, (ignored) -> new HashSet<>())
				.add(templateToConcrete.get(externalTemplate));
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public void finalizer() {
		// Nothing to finalize
	}

	@Override
	public String external(String key) {
		if (!delegate.containsKey(key)) {
			return null;
		}

		return delegate.get(key).iterator().next();
	}

	@Override
	public Collection<String> externalMultiple(String key) {
		if (!delegate.containsKey(key)) {
			return null;
		}

		return delegate.get(key);
	}
}
