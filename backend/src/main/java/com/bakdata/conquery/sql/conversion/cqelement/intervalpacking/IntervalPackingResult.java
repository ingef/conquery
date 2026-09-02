package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import lombok.Value;

@Value
public class IntervalPackingResult {

	QueryStep finalIntervalPackingStep;
	ColumnDateRange aggregatedRange;

}
