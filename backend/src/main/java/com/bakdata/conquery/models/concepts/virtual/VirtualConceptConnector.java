package com.bakdata.conquery.models.concepts.virtual;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VirtualConceptConnector extends Connector {

	private static final long serialVersionUID = 1L;

	@NotNull @NsIdRef
	private Table table;
	@Valid @JsonManagedReference
	private Filter<?> filter;

	@Override
	public List<Filter<?>> collectAllFilters() {
		if(filter == null) {
			return Collections.emptyList();
		}
		return Collections.singletonList(filter);
	}

	@Override
	public void calculateCBlock(CBlock cBlock, Bucket bucket) {
		// This does nothing.
	}
}
