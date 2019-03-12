package com.bakdata.conquery.models.concepts.select.connector.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.PrefixTextAggregator;

import lombok.Getter;
import lombok.Setter;

@CPSType(id = "PREFIX", base = Select.class)
public class PrefixSelect extends SingleColumnSelect {

	@Getter
	@Setter
	private String prefix;

	public PrefixSelect(@NsIdRef Column column, String prefix) {
		super(column);
		this.prefix = prefix;
	}

	@Override
	protected Aggregator<?> createAggregator() {
		return new PrefixTextAggregator(getColumn(), prefix);
	}

	@Override
	public ResultType getResultType() {
		return ResultType.STRING;
	}
}
