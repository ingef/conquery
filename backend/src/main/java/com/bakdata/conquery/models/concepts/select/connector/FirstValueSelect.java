package com.bakdata.conquery.models.concepts.select.connector;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.FirstValueAggregator;

@CPSType(id = "FIRST", base = Select.class)
public class FirstValueSelect extends SingleColumnSelect {

	public FirstValueSelect(@NsIdRef Column column) {
		super(column);
	}

	@Override
	protected Aggregator<?> createAggregator() {
		return new FirstValueAggregator(getColumn());
	}

	@Override
	public ResultType getResultType() {
		return resolveResultType(getColumn().getType());
	}
}
