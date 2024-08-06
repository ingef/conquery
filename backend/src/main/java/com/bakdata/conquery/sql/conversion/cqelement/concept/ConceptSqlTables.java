package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import lombok.Getter;

@Getter
public class ConceptSqlTables extends SqlTables {

	private final List<ConnectorSqlTables> connectorTables;

	public ConceptSqlTables(
			String rootTable,
			Map<CteStep, String> cteNameMap,
			Map<CteStep, CteStep> predecessorMap,
			List<ConnectorSqlTables> connectorTables
	) {
		super(rootTable, cteNameMap, predecessorMap);
		this.connectorTables = connectorTables;
	}

}
