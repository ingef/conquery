package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Set;

import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.SqlTables;

class ConceptTables extends SqlTables<ConceptCteStep> {

	public ConceptTables(String conceptLabel, Set<ConceptCteStep> requiredSteps, String rootTableName, NameGenerator nameGenerator) {
		super(conceptLabel, requiredSteps, rootTableName, nameGenerator);
	}

	public boolean isRequiredStep(ConceptCteStep conceptCteStep) {
		return getCteNames().containsKey(conceptCteStep);
	}

}
