package com.bakdata.conquery.models.concepts.filters;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.IdReference;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;

import lombok.Getter;
import lombok.Setter;

public abstract class SingleColumnFilter<FE_TYPE extends FilterValue<?>> extends Filter<FE_TYPE> implements ISingleColumnFilter {

	@Valid @NotNull @Getter @Setter @IdReference
	private Column column;
	
	@Override
	public Column[] getRequiredColumns() {
		return new Column[]{getColumn()};
	}
}
