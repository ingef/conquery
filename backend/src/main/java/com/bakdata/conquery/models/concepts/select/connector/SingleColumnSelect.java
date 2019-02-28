package com.bakdata.conquery.models.concepts.select.connector;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.ConnectorSelect;
import com.bakdata.conquery.models.datasets.Column;

import lombok.Getter;

public abstract class SingleColumnSelect extends ConnectorSelect {

	@Getter
	@NsIdRef
	@NotNull
	private Column column;
}
