package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import com.bakdata.conquery.models.datasets.concepts.select.connector.LastValueSelect;
import com.bakdata.conquery.sql.conversion.model.select.LastValueSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;

public class LastValueSelectConverter implements SelectConverter<LastValueSelect> {

	@Override
	public SqlSelects convert(LastValueSelect lastSelect, SelectContext context) {
		return LastValueSqlAggregator.create(lastSelect, context).getSqlSelects();
	}

	@Override
	public Class<LastValueSelect> getConversionClass() {
		return LastValueSelect.class;
	}

}
