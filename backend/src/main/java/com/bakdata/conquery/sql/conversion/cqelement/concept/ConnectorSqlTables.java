package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Map;

import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingCteStep;
import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import lombok.Getter;

@Getter
public class ConnectorSqlTables extends SqlTables {

	/**
	 * A unique label for these tables to create unique SQL CTE/Select names.
	 */
	private final String label;

	/**
	 * True if these tables contain interval packing CTEs {@link IntervalPackingCteStep}.
	 */
	private final boolean withIntervalPacking;

	public ConnectorSqlTables(
			String conceptConnectorLabel,
			String rootTable,
			Map<CteStep, String> cteNameMap,
			Map<CteStep, CteStep> predecessorMap,
			boolean containsIntervalPacking
	) {
		super(rootTable, cteNameMap, predecessorMap);
		this.label = conceptConnectorLabel;
		this.withIntervalPacking = containsIntervalPacking;
	}

}
