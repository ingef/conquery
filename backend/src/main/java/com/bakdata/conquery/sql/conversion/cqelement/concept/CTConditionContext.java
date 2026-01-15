package com.bakdata.conquery.sql.conversion.cqelement.concept;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import lombok.Value;

@Value
public class CTConditionContext {

	boolean inFunction;
	String connectorColumn;
	SqlFunctionProvider functionProvider;

	public static CTConditionContext create(Connector connector, SqlFunctionProvider functionProvider) {
		return new CTConditionContext(
				false,
				connector.getColumn() != null ? connector.getColumn().resolve().getName() : null,
				functionProvider
		);
	}

}
