package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.CountSelect;
import com.bakdata.conquery.sql.conversion.model.select.CountSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;

public class CountSelectConverter implements SelectConverter<CountSelect> {

	@Override
	public SqlSelects convert(CountSelect countSelect, SelectContext context) {
		return CountSqlAggregator.create(countSelect, context).getSqlSelects();
	}

	@Override
	public Class<? extends CountSelect> getConversionClass() {
		return CountSelect.class;
	}

}
