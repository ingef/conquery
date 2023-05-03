package com.bakdata.conquery.models.worker;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import javax.validation.Validator;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;

public interface DatasetRegistry extends Injectable, Closeable {

	Namespace createNamespace(Dataset dataset, Validator validator) throws IOException;

	Namespace createNamespace(NamespaceStorage datasetStorage);

	void add(Namespace ns);

	Namespace get(DatasetId dataset);

	void removeNamespace(DatasetId id);

	CentralRegistry findRegistry(DatasetId dataset) throws NoSuchElementException;

	CentralRegistry getMetaRegistry();

	List<Dataset> getAllDatasets();

	Collection<? extends Namespace> getDatasets();

	void close();

	@Override
	MutableInjectableValues inject(MutableInjectableValues values);

	ConqueryConfig getConfig();

	MetaStorage getMetaStorage();

	void setMetaStorage(MetaStorage metaStorage);

	<ID extends Id<T> & NamespacedId, T extends Identifiable<?>> T resolve(ID id);
}
