package com.bakdata.conquery.mode;

import javax.validation.Validator;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import io.dropwizard.setup.Environment;

/**
 * Provider for {@link Manager}.
 */
public interface ManagerProvider {

	String JOB_MANAGER_NAME = "ManagerNode";

	Manager provideManager(ConqueryConfig config, Environment environment);

	static JobManager newJobManager(ConqueryConfig config) {
		return new JobManager(JOB_MANAGER_NAME, config.isFailOnError());
	}

	static InternalObjectMapperCreator newInternalObjectMapperCreator(ConqueryConfig config, Validator validator) {
		return new InternalObjectMapperCreator(config, validator);
	}

	static <N extends Namespace> DatasetRegistry<N> createDatasetRegistry(NamespaceHandler<N> namespaceHandler, ConqueryConfig config,
																		  InternalObjectMapperCreator creator) {

		final IndexService indexService = new IndexService(config.getCsv().createCsvParserSettings(), config.getIndex().getEmptyLabel());
		DatasetRegistry<N> datasetRegistry = new DatasetRegistry<>(
				config.getCluster().getEntityBucketSize(),
				config,
				creator,
				namespaceHandler,
				indexService
		);
		MetaStorage storage = new MetaStorage(config.getStorage(), datasetRegistry);
		datasetRegistry.setMetaStorage(storage);
		return datasetRegistry;
	}

}
