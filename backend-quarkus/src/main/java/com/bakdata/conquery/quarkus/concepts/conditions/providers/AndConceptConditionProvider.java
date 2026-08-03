package com.bakdata.conquery.quarkus.concepts.conditions.providers;

import com.bakdata.conquery.quarkus.concepts.conditions.definitions.AndConceptCondition;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AndConceptConditionProvider extends AbstractConceptConditionProvider<AndConceptCondition> {

	public AndConceptConditionProvider() {
		super(AndConceptCondition.class);
	}

	@Override
	public String type() {
		return "AND";
	}
}
