package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import org.jooq.Condition;

public interface FilterConverter<F extends Filter<V>, V> {

	SqlFilters convertToSqlFilter(F filter, FilterContext<V> filterContext);

	Condition convertForTableExport(F filter, FilterContext<V> filterContext);

}
