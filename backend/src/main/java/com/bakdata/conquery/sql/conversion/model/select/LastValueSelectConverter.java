package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.select.connector.LastValueSelect;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import org.jooq.Field;

public class LastValueSelectConverter implements SelectConverter<LastValueSelect> {

	@Override
	public ConnectorSqlSelects connectorSelect(LastValueSelect select, SelectContext<ConnectorSqlTables> selectContext) {
		return ValueSelectUtil.createValueSelect(
				select.getColumn().resolve(),
				selectContext.getNameGenerator().legacyOperationName(select.getName()),
				Field::desc, select.getSubstringRange(), selectContext
		);
	}
}
