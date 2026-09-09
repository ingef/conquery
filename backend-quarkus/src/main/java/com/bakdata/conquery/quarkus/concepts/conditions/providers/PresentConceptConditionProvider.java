package com.bakdata.conquery.quarkus.concepts.conditions.providers;

import com.bakdata.conquery.quarkus.concepts.conditions.definitions.PresentConceptCondition;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PresentConceptConditionProvider extends AbstractConceptConditionProvider<PresentConceptCondition> {

	public PresentConceptConditionProvider() {
		super(PresentConceptCondition.class);
	}

}
