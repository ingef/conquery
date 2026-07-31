package com.bakdata.conquery.quarkus.models;

public interface PolymorphicModelTypeProvider<B, T extends B> {

	Class<B> baseType();

	String typeId();

	Class<T> modelType();
}
