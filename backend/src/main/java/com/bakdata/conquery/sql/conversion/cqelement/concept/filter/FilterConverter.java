package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.sql.conversion.Converter;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;

public interface FilterConverter<V, F extends Filter<V>> extends Converter<F, SqlFilters, FilterContext<V>> {

}
