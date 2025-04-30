package com.bakdata.conquery.models.datasets;

import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
@JsonIgnoreProperties({"searchDisabled", "generateSearchSuffixes", "searchMinSuffixLength"})
public class SecondaryIdDescription extends NamespacedIdentifiable<SecondaryIdDescriptionId> {

	private DatasetId dataset;

	private String description;

	@View.ApiManagerPersistence
	private InternToExternMapperId mapping;

	/**
	 * If true, SecondaryId will not be displayed to the user or listed in APIs.
	 */
	private boolean hidden = false;

	@Override
	public SecondaryIdDescriptionId createId() {
		return new SecondaryIdDescriptionId(dataset, getName());
	}

	@Override
	public String toString() {
		return "SecondaryIdDescription(id = " + getId() + ", label = " + getLabel() + " )";
	}
}
