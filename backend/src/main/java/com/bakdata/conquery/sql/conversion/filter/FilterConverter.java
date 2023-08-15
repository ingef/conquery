package com.bakdata.conquery.sql.conversion.filter;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.sql.conversion.Converter;
import org.jooq.Condition;
import org.jooq.Name;
import org.jooq.impl.DSL;

/**
 * Converts a {@link com.bakdata.conquery.apiv1.query.concept.filter.FilterValue}
 * to a condition for a SQL WHERE clause.
 *
 * @param <F> The type of Filter this converter is responsible for.
 */
public interface FilterConverter<F extends FilterValue<?>> extends Converter<F, Condition> {

	static Name getColumnName(FilterValue<?> filter) {
		// works for now but we might have to distinguish later if we encounter non-SingleColumnFilters
		return DSL.name(((SingleColumnFilter<?>) filter.getFilter()).getColumn().getName());
	}

}
