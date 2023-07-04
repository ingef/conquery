package com.bakdata.conquery.sql.conversion.filter;

import java.util.List;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.sql.conversion.ConverterService;
import org.jooq.Condition;

public class FilterConverterService extends ConverterService<FilterValue<?>, Condition> {

	public FilterConverterService(List<? extends FilterConverter<?>> converters) {
		super(converters);
	}
}
