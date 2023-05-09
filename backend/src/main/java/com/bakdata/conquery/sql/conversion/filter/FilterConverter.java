package com.bakdata.conquery.sql.conversion.filter;

import java.util.Optional;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import org.jooq.Condition;

/**
 * Converts a {@link com.bakdata.conquery.apiv1.query.concept.filter.FilterValue}
 * to a condition for a SQL WHERE clause.
 *
 * @param <F> The type of Filter this converter is responsible for.
 */
public abstract class FilterConverter<F extends FilterValue<?>> {

	private final FilterSelector<F> selector;

	protected FilterConverter(Class<F> filterClass) {
		this.selector = new FilterSelector<>(filterClass);
	}

	public Optional<Condition> convert(final FilterValue<?> filterNode, ConversionContext context) {
		return this.selector.select(filterNode).map(filter -> this.convertFilter(filter, context));
	}

	protected abstract Condition convertFilter(final F filter, ConversionContext context);

	protected String getColumnName(FilterValue<?> filter) {
		// works for now but we might have to distinguish later if we encounter non-SingleColumnFilters
		return ((SingleColumnFilter<?>) filter.getFilter()).getColumn().getName();
	}

}
