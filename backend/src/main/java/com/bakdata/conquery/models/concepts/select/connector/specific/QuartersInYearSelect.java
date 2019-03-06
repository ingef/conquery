package com.bakdata.conquery.models.concepts.select.connector.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.QuartersInYearAggregator;

/**
 * Entity is included when the the number of quarters with events is within a specified range.
 */
@CPSType(id = "QUARTERS_IN_YEAR", base = Select.class)
public class QuartersInYearSelect extends SingleColumnSelect {
	@Override
	protected Aggregator<?> createAggregator() {
		return new QuartersInYearAggregator(getColumn());
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.INTEGER;
	}
}
