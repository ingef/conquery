package com.bakdata.conquery.util.extensions;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import lombok.Getter;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

@Getter
public class WorkerStorageExtension implements BeforeAllCallback, BeforeEachCallback {

	private final WorkerStorage storage = new WorkerStorage(new NonPersistentStoreFactory(), "test_path");

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		storage.openStores(Jackson.MAPPER);
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		storage.clear();
	}
}
