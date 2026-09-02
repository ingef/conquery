package com.bakdata.conquery.quarkus.concepts.conditions.providers;

import com.bakdata.conquery.quarkus.concepts.conditions.ConceptCondition;
import com.bakdata.conquery.quarkus.concepts.conditions.ConceptConditionProvider;

abstract class AbstractConceptConditionProvider<T extends ConceptCondition> implements ConceptConditionProvider<T> {

	private final Class<T> modelType;

	protected AbstractConceptConditionProvider(Class<T> modelType) {
		this.modelType = modelType;
	}

	@Override
	public Class<T> modelType() {
		return modelType;
	}
}
