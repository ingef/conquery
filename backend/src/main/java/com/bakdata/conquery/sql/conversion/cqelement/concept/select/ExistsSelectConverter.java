package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.ExistsSelect;
import com.bakdata.conquery.sql.conversion.model.select.ExistsSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;

public class ExistsSelectConverter implements SelectConverter<ExistsSelect> {

	@Override
	public SqlSelects convert(ExistsSelect existsSelect, SelectContext context) {
		return ExistsSqlAggregator.create(existsSelect, context).getSqlSelects();
	}

	@Override
	public Class<? extends ExistsSelect> getConversionClass() {
		return ExistsSelect.class;
	}
}
