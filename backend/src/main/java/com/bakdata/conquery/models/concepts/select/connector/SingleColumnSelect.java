package com.bakdata.conquery.models.concepts.select.connector;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@NoArgsConstructor
public abstract class SingleColumnSelect extends Select {

	@Getter
	@Setter
	@NsIdRef
	@NotNull
	@NonNull
	private Column column;

	/**
	 * Indicates if the values in the specified column belong to a categorical set
	 * (bounded number of values).
	 */
	@Getter
	@Setter
	private boolean categorical = false;
	
	@Override
	public ResultType getResultType() {
		if(categorical) {
			return ResultType.CATEGORICAL;
		}
		
		return super.getResultType();
	}
}
