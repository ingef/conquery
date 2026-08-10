package com.bakdata.conquery.quarkus.concepts.filters.values;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class FilterValueRegistry {

	@Inject
	Instance<FilterValueProvider<?>> providers;

	private Map<Class<? extends FilterValue>, FilterValueProvider<?>> providersByModelType = Map.of();

	@PostConstruct
	void init() {
		Map<Class<? extends FilterValue>, FilterValueProvider<?>> index = new LinkedHashMap<>();
		for (FilterValueProvider<?> provider : providers) {
			FilterValueProvider<?> previous = index.put(provider.modelType(), provider);
			if (previous != null) {
				throw new IllegalStateException("Duplicate filter value provider for model type " + provider.modelType().getName() + ": " + previous.getClass().getName() + " and " + provider.getClass().getName());
			}
		}
		providersByModelType = Map.copyOf(index);
	}

	public FilterValueProvider<?> require(FilterValue value) {
		return require(value.getClass());
	}

	public FilterValueProvider<?> require(Class<? extends FilterValue> modelType) {
		FilterValueProvider<?> provider = providersByModelType.get(modelType);
		if (provider == null) {
			throw new IllegalArgumentException("No filter value provider registered for " + modelType.getName());
		}
		return provider;
	}
}
