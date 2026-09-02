package com.bakdata.conquery.quarkus.models;

public interface PolymorphicModelTypeProvider<B, T extends B> {

	Class<T> modelType();

	@SuppressWarnings("unchecked")
	default Class<B> baseType() {
		return (Class<B>) subtype().base();
	}

	default String typeId() {
		return subtype().id();
	}

	private PolymorphicModelSubtype subtype() {
		PolymorphicModelSubtype subtype = modelType().getAnnotation(PolymorphicModelSubtype.class);
		if (subtype == null) {
			throw new IllegalStateException("Polymorphic model " + modelType().getName()
					+ " must be annotated with @PolymorphicModelSubtype");
		}
		return subtype;
	}
}
