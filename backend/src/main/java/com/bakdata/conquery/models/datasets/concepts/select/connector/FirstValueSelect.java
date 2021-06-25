package com.bakdata.conquery.models.datasets.concepts.select.connector;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.FirstValueAggregator;
import com.fasterxml.jackson.annotation.JsonCreator;

@CPSType(id = "FIRST", base = Select.class)
public class FirstValueSelect extends SingleColumnSelect {

	@JsonCreator
	public FirstValueSelect(@NsIdRef Column column) {
		super(column);
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new FirstValueAggregator<>(getColumn());
	}
}
