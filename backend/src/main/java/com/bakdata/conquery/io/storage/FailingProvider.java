package com.bakdata.conquery.io.storage;

import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FailingProvider implements NamespacedStorageProvider {

	public final static FailingProvider INSTANCE = new FailingProvider();
	public static final String ERROR_MSG = "Cannot be used in this environment. This id '%s' cannot be resolved on this node.";

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(NamespacedStorageProvider.class, this);
	}

	@Override
	public NamespacedStorage getStorage(DatasetId datasetId) {
		throw new UnsupportedOperationException(ERROR_MSG.formatted(datasetId));
	}
}
