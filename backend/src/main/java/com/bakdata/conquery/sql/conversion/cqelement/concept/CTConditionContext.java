package com.bakdata.conquery.sql.conversion.cqelement.concept;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import lombok.Value;
import org.jooq.Field;

@Value
public class CTConditionContext {

	Field<String> connectorColumn;
	SqlFunctionProvider functionProvider;

	public static CTConditionContext create(Connector connector, SqlFunctionProvider functionProvider) {
		return new CTConditionContext(
				connector.getColumn() != null ? field(name(connector.getColumn().resolve().getName()), String.class) : null,
				functionProvider
		);
	}

}
