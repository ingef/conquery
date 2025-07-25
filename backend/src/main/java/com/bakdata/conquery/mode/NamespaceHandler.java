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
import io.dropwizard.core.setup.Environment;

/**
 * Handler of namespaces in a ConQuery instance.
 *
 * @param <N> type of the namespace.
 */
public interface NamespaceHandler<N extends Namespace> {

	/**
	 * Creates the {@link NamespaceSetupData} that is shared by all {@link Namespace} types.
	 */
	static NamespaceSetupData createNamespaceSetup(NamespaceStorage storage, final ConqueryConfig config, final InternalMapperFactory internalMapperFactory, DatasetRegistry<?> datasetRegistry, Environment environment) {
		List<Injectable> injectables = new ArrayList<>();
		injectables.add(datasetRegistry);
		injectables.add(storage);

		ObjectMapper persistenceMapper = internalMapperFactory.createNamespacePersistenceMapper(storage, datasetRegistry);
		ObjectMapper preprocessMapper = internalMapperFactory.createPreprocessMapper(storage, datasetRegistry);

		injectables.forEach(i -> {
			i.injectInto(persistenceMapper);
			i.injectInto(preprocessMapper);
		});


		// Each store needs its own mapper because each injects its own registry
		storage.openStores(Jackson.copyMapperAndInjectables(persistenceMapper));

		storage.loadKeys();

		if (config.getStorage().isLoadStoresOnStart()) {
			storage.loadData();
		}

		JobManager jobManager = new JobManager(storage.getDataset().getName(), config.isFailOnError());
		FilterSearch filterSearch = new FilterSearch(config.getIndex());

		return new NamespaceSetupData(preprocessMapper, jobManager, filterSearch);
	}

	N createNamespace(NamespaceStorage namespaceStorage, MetaStorage metaStorage, DatasetRegistry<N> datasetRegistry, Environment environment);

	void removeNamespace(DatasetId id, N namespace);

}
