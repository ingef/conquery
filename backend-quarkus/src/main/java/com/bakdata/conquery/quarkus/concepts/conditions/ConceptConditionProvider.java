package com.bakdata.conquery.quarkus.concepts.conditions;

import com.bakdata.conquery.quarkus.models.PolymorphicModelTypeProvider;

public interface ConceptConditionProvider<T extends ConceptCondition> extends PolymorphicModelTypeProvider<ConceptCondition, T> {

	String type();

	Class<T> conditionType();

	@Override
	default Class<ConceptCondition> baseType() {
		return ConceptCondition.class;
	}

	@Override
	default String typeId() {
		return type();
	}

	@Override
	default Class<T> modelType() {
		return conditionType();
	}
}
