package com.bakdata.conquery.mode.local;

import java.io.InputStream;

import com.bakdata.conquery.mode.ImportHandler;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.worker.Namespace;

public class FailingImportHandler implements ImportHandler {

	@Override
	public void updateImport(Namespace namespace, InputStream inputStream) {
		fail();
	}

	@Override
	public void addImport(Namespace namespace, InputStream inputStream) {
		fail();
	}

	@Override
	public void deleteImport(Import imp) {
		fail();
	}

	private static void fail() {
		throw new UnsupportedOperationException("Imports are not supported when running in SQL mode");
	}
}
