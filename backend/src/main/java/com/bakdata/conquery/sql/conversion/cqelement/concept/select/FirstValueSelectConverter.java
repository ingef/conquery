package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import com.bakdata.conquery.models.datasets.concepts.select.connector.FirstValueSelect;
import com.bakdata.conquery.sql.conversion.model.select.FirstValueSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;

public class FirstValueSelectConverter implements SelectConverter<FirstValueSelect> {

	@Override
	public SqlSelects convert(FirstValueSelect firstSelect, SelectContext context) {
		return FirstValueSqlAggregator.create(firstSelect, context).getSqlSelects();
	}

	@Override
	public Class<FirstValueSelect> getConversionClass() {
		return FirstValueSelect.class;
	}

}
