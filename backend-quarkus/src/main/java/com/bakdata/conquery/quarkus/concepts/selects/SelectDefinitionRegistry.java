package com.bakdata.conquery.quarkus.concepts.selects;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class SelectDefinitionRegistry {

	@Inject
	Instance<SelectDefinitionProvider<?>> providers;

	private Map<Class<? extends SelectDefinition>, SelectDefinitionProvider<?>> providersByModelType = Map.of();

	@PostConstruct
	void init() {
		Map<Class<? extends SelectDefinition>, SelectDefinitionProvider<?>> index = new LinkedHashMap<>();
		for (SelectDefinitionProvider<?> provider : providers) {
			SelectDefinitionProvider<?> previous = index.put(provider.modelType(), provider);
			if (previous != null) {
				throw new IllegalStateException("Duplicate select provider for model type " + provider.modelType().getName() + ": " + previous.getClass().getName() + " and " + provider.getClass().getName());
			}
		}
		providersByModelType = Map.copyOf(index);
	}

	public Optional<SelectDefinitionProvider<?>> find(SelectDefinition definition) {
		return Optional.ofNullable(providersByModelType.get(definition.getClass()));
	}
}
