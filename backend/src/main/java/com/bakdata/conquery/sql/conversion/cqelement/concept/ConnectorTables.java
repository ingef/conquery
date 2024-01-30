package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Set;

import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.SqlTables;

class ConnectorTables extends SqlTables<ConnectorCteStep> {

	public ConnectorTables(String conceptLabel, Set<ConnectorCteStep> requiredSteps, String rootTableName, NameGenerator nameGenerator) {
		super(conceptLabel, requiredSteps, rootTableName, nameGenerator);
	}

}
