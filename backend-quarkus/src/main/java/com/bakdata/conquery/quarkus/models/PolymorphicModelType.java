package com.bakdata.conquery.quarkus.models;

public record PolymorphicModelType<B, T extends B>(
		Class<B> baseType,
		String typeId,
		Class<T> modelType
) {
}
