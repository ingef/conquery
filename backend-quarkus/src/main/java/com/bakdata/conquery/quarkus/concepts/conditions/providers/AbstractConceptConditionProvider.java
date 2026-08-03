package com.bakdata.conquery.quarkus.concepts.conditions.providers;

import com.bakdata.conquery.quarkus.concepts.conditions.ConceptCondition;
import com.bakdata.conquery.quarkus.concepts.conditions.ConceptConditionProvider;

abstract class AbstractConceptConditionProvider<T extends ConceptCondition> implements ConceptConditionProvider<T> {

	private final Class<T> conditionType;

	protected AbstractConceptConditionProvider(Class<T> conditionType) {
		this.conditionType = conditionType;
	}

	@Override
	public Class<T> conditionType() {
		return conditionType;
	}
}
