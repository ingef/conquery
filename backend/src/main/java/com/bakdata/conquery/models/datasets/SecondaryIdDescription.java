package com.bakdata.conquery.models.datasets;

import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Setter
@Getter
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class SecondaryIdDescription extends Labeled<SecondaryIdDescriptionId> {

	@JsonBackReference
	private Dataset dataset;

	private String description;

	@Override
	public SecondaryIdDescriptionId createId() {
		return new SecondaryIdDescriptionId(dataset.getId(), getName());
	}
}
