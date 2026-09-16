package com.bakdata.conquery.quarkus.concepts.conditions.providers;

import com.bakdata.conquery.quarkus.concepts.conditions.definitions.ColumnEqualConceptCondition;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ColumnEqualConceptConditionProvider extends AbstractConceptConditionProvider<ColumnEqualConceptCondition> {

	public ColumnEqualConceptConditionProvider() {
		super(ColumnEqualConceptCondition.class);
	}

}
