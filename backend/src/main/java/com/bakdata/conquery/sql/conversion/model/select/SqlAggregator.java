package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;

public interface SqlAggregator {

	SqlSelects getSqlSelects();

	WhereClauses getWhereClauses();

	default SqlFilters getSqlFilters() {
		return new SqlFilters(getSqlSelects(), getWhereClauses());
	}

}
