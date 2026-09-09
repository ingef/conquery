package com.bakdata.conquery.quarkus.concepts.conditions.providers;

import com.bakdata.conquery.quarkus.concepts.conditions.definitions.OrConceptCondition;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrConceptConditionProvider extends AbstractConceptConditionProvider<OrConceptCondition> {

	public OrConceptConditionProvider() {
		super(OrConceptCondition.class);
	}

}
