package com.bakdata.conquery.models.concepts.filters;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import lombok.Getter;
import lombok.Setter;

public abstract class SingleColumnFilter<FE_TYPE> extends Filter<FE_TYPE> implements ISingleColumnFilter {

	@Valid @NotNull @Getter @Setter @NsIdRef
	private Column column;
	
	@Override
	public final Column[] getRequiredColumns() {
		return new Column[]{getColumn()};
	}
}
