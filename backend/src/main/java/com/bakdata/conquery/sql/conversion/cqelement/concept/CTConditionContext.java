package com.bakdata.conquery.sql.conversion.cqelement.concept;

import static org.jooq.impl.DSL.name;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import lombok.Value;
import org.jooq.Name;

@Value
public class CTConditionContext {

	public static final String COLUMN_VALUE_FIELD = "col_val";
	Name connectorColumn;
	SqlFunctionProvider functionProvider;
	boolean forConnector;

	public static CTConditionContext forJoinTables(SqlFunctionProvider functionProvider) {
		return new CTConditionContext(name(COLUMN_VALUE_FIELD), functionProvider, false);
	}

	public static CTConditionContext forConnector(Connector connector, SqlFunctionProvider functionProvider) {
		return new CTConditionContext(
			connector.getColumn() != null ? name(
				connector.resolveTableId().getTable(),
				connector.getColumn().getColumn()) : null,
			functionProvider,
			true
		);
	}

}
