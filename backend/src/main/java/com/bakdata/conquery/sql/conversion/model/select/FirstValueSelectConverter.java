package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.select.connector.FirstValueSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import org.jooq.Field;

public class FirstValueSelectConverter implements SelectConverter<FirstValueSelect> {

	@Override
	public ConnectorSqlSelects connectorSelect(FirstValueSelect select, SelectContext<ConnectorSqlTables> selectContext) {
		return ValueSelectUtil.createValueSelect(
				select.getColumn().resolve(),
				selectContext.getNameGenerator().selectName(select),
				Field::asc, selectContext
		);
	}

}
