package com.bakdata.conquery.sql.conversion.cqelement.concept;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorCtePlan;
import com.bakdata.conquery.sql.compiler.ir.interval.IntervalPackingCteStep;
import lombok.Getter;

@Getter
public class ConnectorSqlTables extends SqlTables {

	/**
	 * A unique label for these tables to create unique SQL CTE/Select names.
	 */
	private final String name;

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
			String connectorName,
			ConnectorCtePlan plan
	) {
		super(plan.tables());
		this.connector = connector;
		this.name = connectorName;
		this.withIntervalPacking = plan.withIntervalPacking();
		this.excludedFromTimeAggregation = plan.excludedFromTimeAggregation();
	}

}
