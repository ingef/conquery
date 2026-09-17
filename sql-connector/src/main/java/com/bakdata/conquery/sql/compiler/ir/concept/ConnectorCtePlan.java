package com.bakdata.conquery.sql.compiler.ir.concept;

import java.util.Objects;

import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import org.jooq.Record;
import org.jooq.Table;

/** Connector-owned CTE graph together with the execution flags derived while planning it. */
public record ConnectorCtePlan(
		String connectorName,
		Table<Record> sourceTable,
		SqlTables tables,
		boolean withIntervalPacking,
		boolean excludedFromTimeAggregation
) {

	public ConnectorCtePlan {
		Objects.requireNonNull(connectorName, "connectorName");
		Objects.requireNonNull(sourceTable, "sourceTable");
		Objects.requireNonNull(tables, "tables");
	}
}
