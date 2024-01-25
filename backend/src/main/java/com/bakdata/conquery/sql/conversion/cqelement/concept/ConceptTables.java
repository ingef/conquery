package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Set;

import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.SqlTables;

class ConceptTables extends SqlTables<ConnectorCteStep> {

	public ConceptTables(String conceptLabel, Set<ConnectorCteStep> requiredSteps, String rootTableName, NameGenerator nameGenerator) {
		super(conceptLabel, requiredSteps, rootTableName, nameGenerator);
	}

	public boolean isRequiredStep(ConnectorCteStep connectorCteStep) {
		return getCteNames().containsKey(connectorCteStep);
	}

}
