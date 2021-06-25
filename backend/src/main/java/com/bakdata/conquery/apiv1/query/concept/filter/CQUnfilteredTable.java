package com.bakdata.conquery.apiv1.query.concept.filter;

import javax.annotation.Nullable;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CQUnfilteredTable {
	@JsonProperty("id")
	@NsIdRef
	private Connector table;

	@Nullable
	private ValidityDateContainer dateColumn;

}
