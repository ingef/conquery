package com.bakdata.conquery.util.extentions;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.codahale.metrics.MetricRegistry;
import lombok.Getter;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

@Getter
public class NamespaceStorageExtension implements BeforeAllCallback {

	private final NamespaceStorage storage = new NamespaceStorage(new NonPersistentStoreFactory(), "test_path", null);

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		storage.openStores(null, new MetricRegistry());
	}
}
