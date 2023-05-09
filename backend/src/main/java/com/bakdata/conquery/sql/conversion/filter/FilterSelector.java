package com.bakdata.conquery.sql.conversion.filter;

import java.util.Optional;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;

/**
 * Determines if a specific {@link FilterConverter FilterConverter} is responsible
 * for a specific type of {@link com.bakdata.conquery.models.datasets.concepts.filters.Filter Filter}.
 */
public class FilterSelector<F extends FilterValue<?>> {

	private final Class<F> filterClass;

	public FilterSelector(Class<F> filterClass) {
		this.filterClass = filterClass;
	}

	public Optional<F> select(FilterValue<?> filerValue) {
		return filterClass.isInstance(filerValue)
			   ? Optional.of(filterClass.cast(filerValue))
			   : Optional.empty();
	}

}
