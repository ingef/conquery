package com.bakdata.conquery.models.concepts.select.connector;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.types.MajorTypeId;

import lombok.Getter;
import lombok.Setter;

public abstract class SingleColumnSelect extends Select {

	@Getter
	@Setter
	@NsIdRef
	@NotNull
	private Column column;
	
	@Override
	public abstract ResultType getResultType();

	public static ResultType resolveResultType(MajorTypeId majorTypeId) {
		switch (majorTypeId) {
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
				throw new IllegalStateException(String.format("Invalid column type '%s'", majorTypeId));
		}
	}
}
