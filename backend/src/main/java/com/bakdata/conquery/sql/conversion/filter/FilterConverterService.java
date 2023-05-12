package com.bakdata.conquery.sql.conversion.filter;

import java.util.List;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.sql.conversion.ConverterService;
import org.jooq.Condition;

public class FilterConverterService extends ConverterService<FilterValue<?>, Condition> {

	private static final List<? extends FilterConverter<?>> converters = List.of(new RealRangeConverter(), new MultiSelectConverter());

	public FilterConverterService() {
		super(converters);
	}
}
