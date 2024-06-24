package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.sql.conversion.model.CteStep;
import lombok.Getter;

@Getter
public class ConceptSqlTables extends ConnectorSqlTables {

	private final List<ConnectorSqlTables> connectorTables;

	public ConceptSqlTables(
			String conceptConnectorLabel,
			String rootTable,
			Map<CteStep, String> cteNameMap,
			Map<CteStep, CteStep> predecessorMap,
			boolean containsIntervalPacking,
			List<ConnectorSqlTables> connectorTables
	) {
		super(conceptConnectorLabel, rootTable, cteNameMap, predecessorMap, containsIntervalPacking);
		this.connectorTables = connectorTables;
	}

}
