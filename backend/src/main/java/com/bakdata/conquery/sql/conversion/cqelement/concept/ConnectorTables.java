package com.bakdata.conquery.sql.conversion.cqelement.concept;

import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.SqlTables;

class ConnectorTables extends SqlTables<ConnectorCteStep> {

	public ConnectorTables(String conceptLabel, String rootTableName, NameGenerator nameGenerator) {
		super(conceptLabel, ConnectorCteStep.MANDATORY_STEPS, rootTableName, nameGenerator);
	}

}
