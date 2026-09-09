package com.bakdata.conquery.quarkus.models;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class PolymorphicModelRegistry {

	@Inject
	Instance<PolymorphicModelTypeProvider<?, ?>> providers;
	@Inject
	Instance<com.bakdata.conquery.quarkus.plugin.api.models.PolymorphicModelTypeProvider<?, ?>> pluginProviders;

	private Map<Class<?>, Map<String, PolymorphicModelType<?, ?>>> typesByBase = Map.of();

	@PostConstruct
	void init() {
		Map<Class<?>, Map<String, PolymorphicModelType<?, ?>>> index = new LinkedHashMap<>();
		for (PolymorphicModelTypeProvider<?, ?> provider : providers) {
			register(index, provider);
		}
		for (com.bakdata.conquery.quarkus.plugin.api.models.PolymorphicModelTypeProvider<?, ?> provider : pluginProviders) {
			register(index, provider);
		}
		Map<Class<?>, Map<String, PolymorphicModelType<?, ?>>> immutableIndex = new LinkedHashMap<>();
		index.forEach((base, types) -> immutableIndex.put(base, Map.copyOf(types)));
		typesByBase = Map.copyOf(immutableIndex);
	}

	public List<PolymorphicModelType<?, ?>> allTypes() {
		return typesByBase.values().stream()
				.flatMap(types -> types.values().stream())
				.sorted(Comparator.comparing(type -> type.baseType().getName() + ":" + type.typeId()))
				.toList();
	}

	public <B> List<PolymorphicModelType<B, ? extends B>> types(Class<B> baseType) {
		List<PolymorphicModelType<B, ? extends B>> result = new ArrayList<>();
		for (PolymorphicModelType<?, ?> type : typesByBase.getOrDefault(baseType, Map.of()).values()) {
			result.add(cast(type));
		}
		result.sort(Comparator.comparing(PolymorphicModelType::typeId));
		return List.copyOf(result);
	}

	public <B> Optional<PolymorphicModelType<B, ? extends B>> find(Class<B> baseType, String typeId) {
		return Optional.ofNullable(typesByBase.getOrDefault(baseType, Map.of()).get(typeId)).map(this::cast);
	}

	private void register(Map<Class<?>, Map<String, PolymorphicModelType<?, ?>>> index, PolymorphicModelTypeProvider<?, ?> provider) {
		register(index, provider.baseType(), provider.modelType(), provider.typeId(), descriptor(provider));
	}

	private void register(
			Map<Class<?>, Map<String, PolymorphicModelType<?, ?>>> index,
			com.bakdata.conquery.quarkus.plugin.api.models.PolymorphicModelTypeProvider<?, ?> provider
	) {
		register(index, provider.baseType(), provider.modelType(), provider.typeId(), descriptor(provider));
	}

	private void register(
			Map<Class<?>, Map<String, PolymorphicModelType<?, ?>>> index,
			Class<?> baseType,
			Class<?> modelType,
			String typeId,
			PolymorphicModelType<?, ?> descriptor
	) {
		if (typeId == null || typeId.isBlank()) {
			throw new IllegalStateException("Polymorphic model type id must not be blank for " + modelType.getName());
		}
		if (!baseType.isAssignableFrom(modelType)) {
			throw new IllegalStateException("Polymorphic model " + modelType.getName() + " does not implement " + baseType.getName());
		}
		if (baseType.getAnnotation(PolymorphicModelBase.class) == null
				&& baseType.getAnnotation(com.bakdata.conquery.quarkus.plugin.api.models.PolymorphicModelBase.class) == null) {
			throw new IllegalStateException("Polymorphic model base " + baseType.getName() + " must be annotated with @PolymorphicModelBase");
		}
		PolymorphicModelType<?, ?> previous = index.computeIfAbsent(baseType, ignored -> new LinkedHashMap<>()).put(typeId, descriptor);
		if (previous != null) {
			throw new IllegalStateException("Duplicate polymorphic model type '" + typeId + "' for " + baseType.getName() + ": " + previous.modelType().getName() + " and " + modelType.getName());
		}
	}

	private <B, T extends B> PolymorphicModelType<B, T> descriptor(PolymorphicModelTypeProvider<B, T> provider) {
		return new PolymorphicModelType<>(provider.baseType(), provider.typeId(), provider.modelType());
	}

	private <B, T extends B> PolymorphicModelType<B, T> descriptor(
			com.bakdata.conquery.quarkus.plugin.api.models.PolymorphicModelTypeProvider<B, T> provider
	) {
		return new PolymorphicModelType<>(provider.baseType(), provider.typeId(), provider.modelType());
	}

	@SuppressWarnings("unchecked")
	private <B> PolymorphicModelType<B, ? extends B> cast(PolymorphicModelType<?, ?> type) {
		return (PolymorphicModelType<B, ? extends B>) type;
	}
}
