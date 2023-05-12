package com.bakdata.conquery.sql.conversion.filter;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import org.jooq.Condition;
import org.jooq.impl.DSL;

public class MultiSelectConverter implements FilterConverter<FilterValue.CQBigMultiSelectFilter> {

	@Override
	public Condition convert(FilterValue.CQBigMultiSelectFilter filter, ConversionContext context) {
		return DSL.field(FilterConverter.getColumnName(filter)).in(filter.getValue());
	}

	@Override
	public Class<FilterValue.CQBigMultiSelectFilter> getConversionClass() {
		return FilterValue.CQBigMultiSelectFilter.class;
	}
}
