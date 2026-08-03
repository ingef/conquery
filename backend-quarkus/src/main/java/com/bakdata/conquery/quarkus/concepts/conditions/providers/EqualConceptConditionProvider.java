package com.bakdata.conquery.quarkus.concepts.conditions.providers;

import com.bakdata.conquery.quarkus.concepts.conditions.definitions.EqualConceptCondition;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EqualConceptConditionProvider extends AbstractConceptConditionProvider<EqualConceptCondition> {

	public EqualConceptConditionProvider() {
		super(EqualConceptCondition.class);
	}

	@Override
	public String type() {
		return "EQUAL";
	}
}
