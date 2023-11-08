package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Set;

import com.bakdata.conquery.sql.conversion.model.SqlTables;

class ConceptTables extends SqlTables<ConceptStep> {

	public ConceptTables(String conceptLabel, Set<ConceptStep> requiredSteps, String rootTableName) {
		super(conceptLabel, requiredSteps, rootTableName);
	}

	public boolean isRequiredStep(ConceptStep conceptStep) {
		return this.cteNames.containsKey(conceptStep);
	}

}
