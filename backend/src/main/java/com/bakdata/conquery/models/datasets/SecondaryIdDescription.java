package com.bakdata.conquery.models.datasets;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
public class SecondaryIdDescription extends Labeled<SecondaryIdId> {

	@NsIdRef
	private final Dataset dataset;

	private final String description;

	@Override
	public SecondaryIdId createId() {
		return new SecondaryIdId(dataset.getId(),getName());
	}
}
