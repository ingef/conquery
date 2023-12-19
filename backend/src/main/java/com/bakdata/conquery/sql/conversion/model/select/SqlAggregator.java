package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.sql.conversion.model.filter.Filters;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;

public interface SqlAggregator {

	SqlSelects getSqlSelects();

	Filters getFilters();

	default SqlFilters getSqlFilters() {
		return new SqlFilters(getSqlSelects(), getFilters());
	}

}
