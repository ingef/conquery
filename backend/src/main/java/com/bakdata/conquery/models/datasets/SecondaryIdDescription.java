package com.bakdata.conquery.models.datasets;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
public class SecondaryIdDescription extends Labeled<SecondaryIdDescriptionId> implements NamespacedIdentifiable<SecondaryIdDescriptionId> {

	@NsIdRef
	private Dataset dataset;

	private String description;

	@Override
	public SecondaryIdDescriptionId createId() {
		return new SecondaryIdDescriptionId(dataset.getId(), getName());
	}

	@Override
	public String toString() {
		return "SecondaryIdDescription(id = " + getId() + ", label = " + getLabel() + " )";
	}
}
