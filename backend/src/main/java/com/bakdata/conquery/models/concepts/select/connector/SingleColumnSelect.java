package com.bakdata.conquery.models.concepts.select.connector;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.ConnectorSelect;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.externalservice.ResultType;

import lombok.Getter;

public abstract class SingleColumnSelect extends ConnectorSelect {

	@Getter
	@NsIdRef
	@NotNull
	private Column column;
	
	@Override
	public ResultType getResultType() {
		switch (getColumn().getType()) {
			case STRING:
				return ResultType.STRING;
			case BOOLEAN:
				return ResultType.BOOLEAN;
			case DATE:
				return ResultType.DATE;
			case DATE_RANGE:
				return ResultType.STRING;
			case INTEGER:
				return ResultType.INTEGER;
			case MONEY:
				return ResultType.MONEY;
			case DECIMAL:
			case REAL:
				return ResultType.NUMERIC;
			default:
				throw new IllegalStateException(String.format("Invalid column type '%s' for Aggregator", getColumn().getType()));
		}
	}
}
