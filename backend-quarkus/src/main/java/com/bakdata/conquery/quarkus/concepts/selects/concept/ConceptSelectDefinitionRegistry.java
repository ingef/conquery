package com.bakdata.conquery.quarkus.concepts.selects.concept;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConceptSelectDefinitionRegistry {

	@Inject
	Instance<ConceptSelectDefinitionProvider<?>> providers;

	private Map<Class<? extends ConceptSelectDefinition>, ConceptSelectDefinitionProvider<?>> providersByModelType = Map.of();

	@PostConstruct
	void init() {
		Map<Class<? extends ConceptSelectDefinition>, ConceptSelectDefinitionProvider<?>> index = new LinkedHashMap<>();
		for (ConceptSelectDefinitionProvider<?> provider : providers) {
			ConceptSelectDefinitionProvider<?> previous = index.put(provider.payloadType(), provider);
			if (previous != null) {
				throw new IllegalStateException("Duplicate concept select provider for model type " + provider.payloadType().getName() + ": " + previous.getClass().getName() + " and " + provider.getClass().getName());
			}
		}
		providersByModelType = Map.copyOf(index);
	}

	public Optional<ConceptSelectDefinitionProvider<?>> find(ConceptSelectDefinition definition) {
		return Optional.ofNullable(providersByModelType.get(definition.getClass()));
	}
}
