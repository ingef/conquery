package com.bakdata.conquery.sql.translate.query.concept;

import com.bakdata.conquery.sql.translate.ir.SqlTables;
import lombok.NonNull;
import org.jooq.Record;
import org.jooq.Table;

/** Connector-owned CTE graph together with the execution flags derived while planning it. */
public record ConnectorCtePlan(
		@NonNull String connectorName,
		@NonNull Table<Record> sourceTable,
		@NonNull SqlTables tables,
		boolean withIntervalPacking
) {
}
