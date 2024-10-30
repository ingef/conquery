package com.bakdata.conquery.models.index;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MapIndex implements Index<String> {

	private final String externalTemplate;
	private final HashMap<String, String> delegate = new HashMap<>();

	@Override
	public void put(String key, Map<String, String> templateToConcrete) {
		final String prior = delegate.putIfAbsent(key, templateToConcrete.get(externalTemplate));
		if (prior != null) {
			throw new IllegalArgumentException("Duplicate entry for key %s".formatted(key));
		}
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
	public Collection<String> externalMultiple(String key, String defaultValue) {
		return Set.of(external(key, defaultValue));
	}

	@Override
	public String external(String key, String defaultValue) {
		return delegate.getOrDefault(key, defaultValue);
	}
}
