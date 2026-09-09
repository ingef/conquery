package com.bakdata.conquery.quarkus.concepts.conditions.providers;

import com.bakdata.conquery.quarkus.concepts.conditions.definitions.PrefixListConceptCondition;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PrefixListConceptConditionProvider extends AbstractConceptConditionProvider<PrefixListConceptCondition> {

	public PrefixListConceptConditionProvider() {
		super(PrefixListConceptCondition.class);
	}

}
