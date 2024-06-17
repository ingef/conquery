package com.bakdata.conquery.sql.conversion.model.aggregator;

import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;

/**
 * Marker interface. SQL aggregators extend {@link SelectConverter} and {@link FilterConverter} and share common code for {@link Select} and
 * {@link Filter} conversion.
 */
interface SqlAggregator {
}
