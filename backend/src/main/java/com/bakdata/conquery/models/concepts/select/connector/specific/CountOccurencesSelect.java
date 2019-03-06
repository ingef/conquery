package com.bakdata.conquery.models.concepts.select.connector.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.MultiSelectAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SelectAggregator;

import lombok.Getter;
import lombok.Setter;

@CPSType(id = "COUNT_OCCURENCES", base = Select.class)
public class CountOccurencesSelect extends SingleColumnSelect {

	@Getter
	@Setter
	private String[] selection;

	@Override
	protected Aggregator<?> createAggregator() {
		if (selection.length == 1) {
			return new SelectAggregator(getColumn(), selection[0]);
		}

		return new MultiSelectAggregator(getColumn(), selection);
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.INTEGER;
	}
}
