package com.bakdata.conquery.quarkus.concepts.filters;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class FilterDefinitionRegistry {

	@Inject
	Instance<FilterDefinitionProvider<?>> providers;

	private Map<String, FilterDefinitionProvider<?>> providersByType = Map.of();

	@PostConstruct
	void init() {
		Map<String, FilterDefinitionProvider<?>> index = new LinkedHashMap<>();
		for (FilterDefinitionProvider<?> provider : providers) {
			FilterDefinitionProvider<?> previous = index.put(provider.type(), provider);
			if (previous != null) {
				throw new IllegalStateException("Duplicate filter type provider for '" + provider.type() + "': " + previous.getClass().getName() + " and " + provider.getClass().getName());
			}
		}
		providersByType = Map.copyOf(index);
	}

	public Optional<FilterDefinitionProvider<?>> find(String type) {
		return Optional.ofNullable(providersByType.get(type));
	}
}
