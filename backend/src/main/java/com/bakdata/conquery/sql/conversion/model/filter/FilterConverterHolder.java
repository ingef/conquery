package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;

/**
 * Glue-class to connect a concrete {@link Filter} class with a {@link FilterConverter}.
 *
 * @param <F> The type of {@link Filter} this {@link FilterConverter} can handle.
 * @param <V> The {@link Filter}s value type.
 */
@RequiredArgsConstructor
public class FilterConverterHolder<F extends Filter<V>, V> {

	private final F filter;
	private final FilterConverter<F, V> converter;

	public SqlFilters convertToSqlFilter(FilterContext<V> filterContext) {
		return converter.convertToSqlFilter(filter, filterContext);
	}

	public Condition convertForTableExport(FilterContext<V> filterContext) {
		return converter.convertForTableExport(filter, filterContext);
	}

}
