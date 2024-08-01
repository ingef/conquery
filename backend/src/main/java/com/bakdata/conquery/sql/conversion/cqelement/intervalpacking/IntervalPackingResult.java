package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import lombok.Value;

@Value
public class IntervalPackingResult {

	QueryStep finalIntervalPackingStep;
	ColumnDateRange aggregatedRange;

}
