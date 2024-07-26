package com.bakdata.conquery.util.extentions;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.codahale.metrics.MetricRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;


@RequiredArgsConstructor
@Getter
public class MetaStorageExtension implements BeforeAllCallback, BeforeEachCallback {

	private final MetaStorage metaStorage = new MetaStorage(new NonPersistentStoreFactory());

	@Override
	public void beforeAll(ExtensionContext extensionContext) throws Exception {
		metaStorage.openStores(Jackson.MAPPER, new MetricRegistry());
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		metaStorage.clear();
	}
}