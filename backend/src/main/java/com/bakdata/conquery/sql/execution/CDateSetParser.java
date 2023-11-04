package com.bakdata.conquery.sql.execution;

import com.bakdata.conquery.models.common.CDateSet;

public interface CDateSetParser {

	CDateSet fromString(String multiDateRange);

}
