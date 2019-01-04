package com.bakdata.conquery.models.concepts.select;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.IdReference;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.select.Select;

import lombok.Getter;

public abstract class ColumnSelect extends Select {

	@Getter
	@IdReference
	@NotNull
	private Column column;
}
