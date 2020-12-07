package com.bakdata.conquery.models.datasets;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
public class SecondaryIdDescription extends Labeled<SecondaryId> {

	@NsIdRef
	private final Dataset dataset;

	private final String description;

	@Override
	public SecondaryId createId() {
		return new SecondaryId(dataset.getId(), getName());
	}
}
