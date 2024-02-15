package com.bakdata.conquery.sql.conversion.model.aggregator;

import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;

public interface SqlAggregator {

	SqlSelects getSqlSelects();

	WhereClauses getWhereClauses();

	default SqlFilters getSqlFilters() {
		return new SqlFilters(getSqlSelects(), getWhereClauses());
	}

}
