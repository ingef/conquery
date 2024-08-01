package com.bakdata.conquery.mode;

import java.io.InputStream;

import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.worker.Namespace;

/**
 * Handler of {@link Import} requests.
 */
public interface ImportHandler {

	void updateImport(Namespace namespace, InputStream inputStream);

	void addImport(Namespace namespace, InputStream inputStream);

	void deleteImport(Import imp);

}
