package com.bakdata.conquery.sql.conversion.cqelement.concept;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import lombok.Value;
import org.jooq.Field;

@Value
public class CTConditionContext {

	private static final Field<String> COLUMN_VALUE_FIELD = field(name("col_val"), String.class);
	Field<String> connectorColumn;
	SqlFunctionProvider functionProvider;

	public static CTConditionContext forJoinTables(SqlFunctionProvider functionProvider) {
		return new CTConditionContext(COLUMN_VALUE_FIELD, functionProvider);
	}

	public static CTConditionContext forConnector(Connector connector, SqlFunctionProvider functionProvider) {
		return new CTConditionContext(
				connector.getColumn() != null ? field(name(connector.getColumn().resolve().getName()), String.class).as(COLUMN_VALUE_FIELD) : null,
				functionProvider
		);
	}

}
