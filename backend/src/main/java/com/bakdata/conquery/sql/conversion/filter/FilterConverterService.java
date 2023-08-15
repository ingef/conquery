package com.bakdata.conquery.sql.conversion.filter;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.sql.conversion.ConverterService;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import java.util.List;

public class FilterConverterService extends ConverterService<FilterValue<?>, Condition> {

	public FilterConverterService(List<? extends FilterConverter<?>> converters) {
		super(converters);
	}

	@Override
	public Condition convert(FilterValue<?> filterValue, ConversionContext context) {
		Condition condition = super.convert(filterValue, context);
		if (!context.isNegation()) {
			return condition;
		}
		return DSL.not(condition);
	}

}
