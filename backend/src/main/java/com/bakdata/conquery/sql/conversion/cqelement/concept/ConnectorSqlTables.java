package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Map;

import com.bakdata.conquery.models.datasets.concepts.Connector;
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

	/**
	 * True if these tables should not propagate a present validity date.
	 */
	private final boolean excludedFromTimeAggregation;

	/**
	 * Corresponding {@link Connector} of these {@link SqlTables}.
	 */
	private final Connector connector;

	public ConnectorSqlTables(
			Connector connector,
			String conceptConnectorLabel,
			String rootTable,
			Map<CteStep, String> cteNameMap,
			Map<CteStep, CteStep> predecessorMap,
			boolean containsIntervalPacking,
			boolean excludedFromTimeAggregation
	) {
		super(rootTable, cteNameMap, predecessorMap);
		this.connector = connector;
		this.label = conceptConnectorLabel;
		this.withIntervalPacking = containsIntervalPacking;
		this.excludedFromTimeAggregation = excludedFromTimeAggregation;
	}

}
