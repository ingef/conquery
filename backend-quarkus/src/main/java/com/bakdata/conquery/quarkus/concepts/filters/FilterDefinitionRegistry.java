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

	private Map<Class<? extends FilterDefinition>, FilterDefinitionProvider<?>> providersByModelType = Map.of();

	@PostConstruct
	void init() {
		Map<Class<? extends FilterDefinition>, FilterDefinitionProvider<?>> index = new LinkedHashMap<>();
		for (FilterDefinitionProvider<?> provider : providers) {
			FilterDefinitionProvider<?> previous = index.put(provider.payloadType(), provider);
			if (previous != null) {
				throw new IllegalStateException("Duplicate filter provider for model type " + provider.payloadType().getName() + ": " + previous.getClass().getName() + " and " + provider.getClass().getName());
			}
		}
		providersByModelType = Map.copyOf(index);
	}

	public Optional<FilterDefinitionProvider<?>> find(FilterDefinition definition) {
		return Optional.ofNullable(providersByModelType.get(definition.getClass()));
	}
}
