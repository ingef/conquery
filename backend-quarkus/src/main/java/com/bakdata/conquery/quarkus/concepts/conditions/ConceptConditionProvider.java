package com.bakdata.conquery.quarkus.concepts.conditions;

import com.bakdata.conquery.quarkus.models.PolymorphicModelTypeProvider;

public interface ConceptConditionProvider<T extends ConceptCondition> extends PolymorphicModelTypeProvider<ConceptCondition, T> {

	default String type() {
		return typeId();
	}
}
