package com.bakdata.conquery.models.datasets;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor(onConstructor_ = {@JsonCreator})
@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class SecondaryIdDescription extends Labeled<SecondaryIdDescriptionId> {

	@NsIdRef
	private Dataset dataset;

	private String description;

	@Override
	public SecondaryIdDescriptionId createId() {
		return new SecondaryIdDescriptionId(dataset.getId(), getName());
	}
}
