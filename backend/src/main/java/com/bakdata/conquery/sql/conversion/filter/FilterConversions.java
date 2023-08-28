package com.bakdata.conquery.sql.conversion.filter;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.sql.conversion.Conversions;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConceptFilter;

public class FilterConversions extends Conversions<Filter<?>, ConceptFilter, FilterContext<?>> {

	public FilterConversions(List<? extends FilterConverter<?, ?>> converters) {
		super(converters);
	}

}
