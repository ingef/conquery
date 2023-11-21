package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import lombok.Value;

@Value
public class SqlFilters {
	SqlSelects selects;
	Filters filters;
}
