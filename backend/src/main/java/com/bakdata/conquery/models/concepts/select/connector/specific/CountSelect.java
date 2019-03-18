package com.bakdata.conquery.models.concepts.select.connector.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountAggregator;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

@CPSType(id = "COUNT", base = Select.class)
public class CountSelect extends SingleColumnSelect {

	@Getter
	@Setter
	private boolean distinct = false;

	@JsonCreator
	public CountSelect(@NsIdRef Column column) {
		this(column, false);
	}

	public CountSelect(@NsIdRef Column column, boolean distinct) {
		super(column);
		this.distinct = distinct;
	}

	@Override
	public Aggregator<?> createAggregator() {
		if (distinct) {
			return new DistinctValuesWrapperAggregator<>(new CountAggregator(getColumn()), getColumn());
		}
		else {
			return new CountAggregator(getColumn());
		}
	}
	

	
	@Override
	public ResultType getResultType() {
		return ResultType.INTEGER;
	}
}
