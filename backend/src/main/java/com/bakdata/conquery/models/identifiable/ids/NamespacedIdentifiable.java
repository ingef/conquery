package com.bakdata.conquery.models.identifiable.ids;

import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;

public abstract class NamespacedIdentifiable<ID extends NamespacedId<?>> extends IdentifiableImpl<ID> {

	public abstract DatasetId getDataset();


	protected NamespacedStorageProvider getStorageProvider() {
		//TODO make field?
		return getDataset().getNamespacedStorageProvider();
	}

	@Override
	protected void injectStore(ID id) {
		NamespacedStorageProvider storageProvider = getStorageProvider();
		id.setNamespacedStorageProvider(storageProvider);
	}
}
