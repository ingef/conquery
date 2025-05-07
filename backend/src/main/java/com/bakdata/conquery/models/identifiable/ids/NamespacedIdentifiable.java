package com.bakdata.conquery.models.identifiable.ids;

import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;

public abstract class NamespacedIdentifiable<ID extends NamespacedId<?>> extends IdentifiableImpl<ID> {

	public abstract DatasetId getDataset();

	protected NamespacedStorageProvider getStorageProvider() {
		return getDataset().getNamespacedStorageProvider();
	}

	@Override
	protected void injectDomain(ID id) {
		// only implemented for DatasetId
	}
}
