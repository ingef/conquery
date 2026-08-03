package com.bakdata.conquery.quarkus.concepts.conditions.providers;

import com.bakdata.conquery.quarkus.concepts.conditions.definitions.PrefixRangeConceptCondition;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PrefixRangeConceptConditionProvider extends AbstractConceptConditionProvider<PrefixRangeConceptCondition> {

	public PrefixRangeConceptConditionProvider() {
		super(PrefixRangeConceptCondition.class);
	}

	@Override
	public String type() {
		return "PREFIX_RANGE";
	}
}
