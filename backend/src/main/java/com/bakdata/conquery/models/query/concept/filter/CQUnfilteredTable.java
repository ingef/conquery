package com.bakdata.conquery.models.query.concept.filter;

import javax.annotation.Nullable;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.Connector;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CQUnfilteredTable {
	@NsIdRef
	private Connector table;

	@Nullable
	private ValidityDateContainer dateColumn;

}
