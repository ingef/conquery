package com.bakdata.conquery.mode;

import com.bakdata.conquery.mode.cluster.InternalMapperFactory;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import io.dropwizard.core.setup.Environment;

/**
 * Provider for {@link Manager}.
 */
public interface ManagerProvider {

	String JOB_MANAGER_NAME = "ManagerNode";

	Manager provideManager(ConqueryConfig config, Environment environment);

	static JobManager newJobManager(ConqueryConfig config) {
		return new JobManager(JOB_MANAGER_NAME, config.isFailOnError());
	}

	static <N extends Namespace> DatasetRegistry<N> createDatasetRegistry(
			NamespaceHandler<N> namespaceHandler,
			ConqueryConfig config,
			InternalMapperFactory internalMapperFactory
	) {
		final IndexService indexService = new IndexService(config.getCsv().createCsvParserSettings(), config.getIndex().getEmptyLabel());
		return new DatasetRegistry<>(
				config,
				internalMapperFactory,
				namespaceHandler,
				indexService
		);
	}

}
