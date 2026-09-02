package com.bakdata.conquery.quarkus.concepts.filters;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValueRegistry;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterDefinition;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterDefinitionProvider;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class FilterDefinitionRegistry {

	@Inject
	Instance<FilterDefinitionProvider<?>> providers;

	@Inject
	FilterValueRegistry filterValueRegistry;

	private Map<Class<? extends FilterDefinition>, FilterDefinitionProvider<?>> providersByModelType = Map.of();

	@PostConstruct
	void init() {
		Map<Class<? extends FilterDefinition>, FilterDefinitionProvider<?>> index = new LinkedHashMap<>();
		for (FilterDefinitionProvider<?> provider : providers) {
			if (provider.acceptedValueTypes().isEmpty()) {
				throw new IllegalStateException("Filter provider " + provider.getClass().getName() + " does not declare any accepted filter value types");
			}
			provider.acceptedValueTypes().forEach(filterValueRegistry::require);
			FilterDefinitionProvider<?> previous = index.put(provider.modelType(), provider);
			if (previous != null) {
				throw new IllegalStateException("Duplicate filter provider for model type " + provider.modelType().getName() + ": " + previous.getClass().getName() + " and " + provider.getClass().getName());
			}
		}
		providersByModelType = Map.copyOf(index);
	}

	public Optional<FilterDefinitionProvider<?>> find(FilterDefinition definition) {
		return Optional.ofNullable(providersByModelType.get(definition.getClass()));
	}
}
