package com.bakdata.conquery.models.query.concept.filter;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.Connector;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CQFilteredTable {
	@JsonProperty("id")
	@NsIdRef
	private Connector connector;

	@Nullable
	private ValidityDateContainer dateColumn;

	private List<FilterValue<?>> filters = Collections.emptyList();

}
