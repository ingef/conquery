package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Set;

import com.bakdata.conquery.sql.conversion.model.SqlTables;

class ConceptTables extends SqlTables<ConceptCteStep> {

	public ConceptTables(String conceptLabel, Set<ConceptCteStep> requiredSteps, String rootTableName) {
		super(conceptLabel, requiredSteps, rootTableName);
	}

	public boolean isRequiredStep(ConceptCteStep conceptCteStep) {
		return this.cteNames.containsKey(conceptCteStep);
	}

}
