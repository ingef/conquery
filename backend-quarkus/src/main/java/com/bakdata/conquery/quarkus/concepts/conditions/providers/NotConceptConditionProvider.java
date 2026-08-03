package com.bakdata.conquery.quarkus.concepts.conditions.providers;

import com.bakdata.conquery.quarkus.concepts.conditions.definitions.NotConceptCondition;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NotConceptConditionProvider extends AbstractConceptConditionProvider<NotConceptCondition> {

	public NotConceptConditionProvider() {
		super(NotConceptCondition.class);
	}

	@Override
	public String type() {
		return "NOT";
	}
}
