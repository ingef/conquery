package com.bakdata.conquery.mode;

import java.util.ArrayList;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.query.FilterSearch;
import com.bakdata.conquery.models.worker.Namespace;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handler of namespaces in a ConQuery instance.
 *
 * @param <N> type of the namespace.
 */
public interface NamespaceHandler<N extends Namespace> {

	N createNamespace(NamespaceStorage storage, MetaStorage metaStorage, IndexService indexService, MetricRegistry metricRegistry, InternalObjectMapperCreator mapperCreator);

	void removeNamespace(DatasetId id, N namespace);

	/**
	 * Creates the {@link NamespaceSetupData} that is shared by all {@link Namespace} types.
	 */
	static NamespaceSetupData createNamespaceSetup(NamespaceStorage storage, final ConqueryConfig config, final InternalObjectMapperCreator mapperCreator, IndexService indexService, MetricRegistry metricRegistry) {
		ArrayList<Injectable> injectables = new ArrayList<>();
		injectables.add(indexService);

		injectables.add(storage);

		Injectable[] injectablesArray = injectables.toArray(Injectable[]::new);

		ObjectMapper persistenceMapper = mapperCreator.createInternalObjectMapper(View.Persistence.Manager.class, injectablesArray);
		ObjectMapper communicationMapper = mapperCreator.createInternalObjectMapper(View.InternalCommunication.class, injectablesArray);
		ObjectMapper preprocessMapper = mapperCreator.createInternalObjectMapper(null, injectablesArray);

		// Each store needs its own mapper because each injects its own NsIdResolver
		storage.openStores(Jackson.copyMapperAndInjectables(persistenceMapper), metricRegistry);

		JobManager jobManager = new JobManager(storage.getDataset().getName(), config.isFailOnError());

		FilterSearch filterSearch = new FilterSearch(config.getIndex());
		return new NamespaceSetupData(injectables, indexService, communicationMapper, preprocessMapper, jobManager, filterSearch);
	}

}
