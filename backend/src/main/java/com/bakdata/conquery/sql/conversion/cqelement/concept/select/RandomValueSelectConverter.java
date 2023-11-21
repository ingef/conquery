package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import com.bakdata.conquery.models.datasets.concepts.select.connector.RandomValueSelect;
import com.bakdata.conquery.sql.conversion.model.select.RandomValueSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;

public class RandomValueSelectConverter implements SelectConverter<RandomValueSelect> {

	@Override
	public SqlSelects convert(RandomValueSelect randomSelect, SelectContext context) {
		return RandomValueSqlAggregator.create(randomSelect, context).getSqlSelects();
	}

	@Override
	public Class<RandomValueSelect> getConversionClass() {
		return RandomValueSelect.class;
	}


}
