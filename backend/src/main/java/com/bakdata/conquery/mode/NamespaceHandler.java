package com.bakdata.conquery.mode;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.cluster.InternalMapperFactory;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.query.FilterSearch;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handler of namespaces in a ConQuery instance.
 *
 * @param <N> type of the namespace.
 */
public interface NamespaceHandler<N extends Namespace> {

	N createNamespace(NamespaceStorage storage, MetaStorage metaStorage, DatasetRegistry<N> datasetRegistry);

	void removeNamespace(DatasetId id, N namespace);

	/**
	 * Creates the {@link NamespaceSetupData} that is shared by all {@link Namespace} types.
	 */
	static NamespaceSetupData createNamespaceSetup(NamespaceStorage storage, final ConqueryConfig config, final InternalMapperFactory internalMapperFactory, DatasetRegistry<?> datasetRegistry) {
		List<Injectable> injectables = new ArrayList<>();
		injectables.add(datasetRegistry);

		ObjectMapper persistenceMapper = internalMapperFactory.createNamespacePersistenceMapper(datasetRegistry);
		ObjectMapper communicationMapper = internalMapperFactory.createManagerCommunicationMapper(datasetRegistry);
		ObjectMapper preprocessMapper = internalMapperFactory.createPreprocessMapper(datasetRegistry);

		injectables.forEach(i -> i.injectInto(persistenceMapper));
		injectables.forEach(i -> i.injectInto(communicationMapper));
		injectables.forEach(i -> i.injectInto(preprocessMapper));


		// Each store needs its own mapper because each injects its own registry
		storage.openStores(Jackson.copyMapperAndInjectables(persistenceMapper));
		storage.loadData();

		JobManager jobManager = new JobManager(storage.getDataset().getName(), config.isFailOnError());

		FilterSearch filterSearch = new FilterSearch(config.getIndex());
		return new NamespaceSetupData(injectables, communicationMapper, preprocessMapper, jobManager, filterSearch);
	}

}
