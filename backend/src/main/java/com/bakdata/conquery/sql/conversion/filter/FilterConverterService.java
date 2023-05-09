package com.bakdata.conquery.sql.conversion.filter;

import java.util.List;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import org.jooq.Condition;

public class FilterConverterService {
	private final List<FilterConverter<? extends FilterValue<?>>> converters = List.of(
			new RealRangeConverter(),
			new MultiSelectConverter()
	);

	public Condition convertNode(FilterValue<?> filterNode, ConversionContext context) {
		return converters.stream()
						 .flatMap(converter -> converter.convert(filterNode, context).stream())
						 .findFirst()
						 .orElseThrow();
	}

}
