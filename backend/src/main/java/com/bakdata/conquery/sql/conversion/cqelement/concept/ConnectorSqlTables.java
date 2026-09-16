package com.bakdata.conquery.sql.conversion.cqelement.concept;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorCtePlan;
import lombok.Getter;

@Getter
public class ConnectorSqlTables extends SqlTables {

	/**
	 * Corresponding {@link Connector} of these {@link SqlTables}.
	 */
	private final Connector connector;
	private final ConnectorCtePlan plan;

	public ConnectorSqlTables(
			Connector connector,
			ConnectorCtePlan plan
	) {
		super(plan.tables());
		this.connector = connector;
		this.plan = plan;
	}

	/** A unique label for these tables to create unique SQL CTE/select names. */
	public String getName() {
		return plan.connectorName();
	}

}
