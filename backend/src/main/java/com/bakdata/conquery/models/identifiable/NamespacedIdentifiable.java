package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JsonIgnore;

public non-sealed abstract class NamespacedIdentifiable<ID extends NamespacedId<?>> extends IdentifiableImpl<ID, NamespacedStorageProvider> {

	public abstract DatasetId getDataset();

	@Override
	@JsonIgnore
	public NamespacedStorageProvider getDomain() {
		return getDataset().getDomain();
	}
}
