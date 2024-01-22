package com.bakdata.conquery.models.worker;

import java.io.Closeable;
import java.util.List;
import java.util.NoSuchElementException;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.FilterSearch;
import com.fasterxml.jackson.databind.ObjectMapper;

public interface Namespace extends Injectable, Closeable {

	Dataset getDataset();

	void remove();

	CentralRegistry getCentralRegistry();

	int getNumberOfEntities();

	void updateInternToExternMappings();

	void clearIndexCache();

	PreviewConfig getPreviewConfig();

	CentralRegistry findRegistry(DatasetId dataset) throws NoSuchElementException;

	CentralRegistry getMetaRegistry();

	ExecutionManager getExecutionManager();

	ObjectMapper getPreprocessMapper();

	ObjectMapper getCommunicationMapper();

	NamespaceStorage getStorage();

	JobManager getJobManager();

	FilterSearch getFilterSearch();

	IndexService getIndexService();

	List<Injectable> getInjectables();

	<ID extends Id<T> & NamespacedId, T extends Identifiable<?>> T resolve(ID id);
}
