package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import lombok.Getter;

@Getter
public class ConceptSqlTables extends SqlTables {

	private final List<ConnectorSqlTables> connectorTables;

	public ConceptSqlTables(
			SqlTables tables,
			List<ConnectorSqlTables> connectorTables
	) {
		super(tables);
		this.connectorTables = connectorTables;
	}

}
