package com.bakdata.conquery.sql.conversion.filter;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import org.jooq.Condition;
import org.jooq.impl.DSL;

public class MultiSelectConverter extends FilterConverter<FilterValue.CQBigMultiSelectFilter> {

	public MultiSelectConverter() {
		super(FilterValue.CQBigMultiSelectFilter.class);
	}

	@Override
	protected Condition convertFilter(FilterValue.CQBigMultiSelectFilter filter, ConversionContext context) {
		return DSL.field(super.getColumnName(filter)).in(filter.getValue());
	}

}
